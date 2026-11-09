import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import fs from 'fs';
import path from 'path';

const WEB_URL = 'http://localhost:5175';
const TOKEN_URL = 'http://localhost:8180/realms/workflow-platform/protocol/openid-connect/token';
const WORKFLOW_XML = fs.readFileSync(path.join(__dirname, 'samples', 'tracker-flow.bpmn20.xml'), 'utf-8');

const projectKey = `T${Date.now() % 10_000_000}`;
const title = `Tracker flow ${projectKey}`;

async function login(page: Page, username: string) {
  await page.goto(WEB_URL);
  await page.getByLabel('Username or email').fill(username);
  await page.getByLabel('Password', { exact: true }).fill('password');
  await page.getByRole('button', { name: 'Sign In' }).click();
  await expect(page.getByRole('heading', { name: 'My work' })).toBeVisible({ timeout: 20000 });
}

async function adminToken(request: APIRequestContext) {
  const response = await request.post(TOKEN_URL, {
    form: { grant_type: 'password', client_id: 'wfp-web', username: 'admin-a', password: 'password' },
  });
  expect(response.status(), await response.text()).toBe(200);
  return (await response.json()).access_token;
}

function column(page: Page, name: string) {
  return page.getByRole('region', { name, exact: true });
}

test.describe.serial('Tracker', () => {
  test('an admin deploys a workflow and creates a project', async ({ page, request }) => {
    const token = await adminToken(request);
    const deployment = await request.post(`${WEB_URL}/api/workflow/deployments`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { name: 'trackerFlow', bpmnXml: WORKFLOW_XML },
    });
    expect(deployment.status()).toBe(201);

    await login(page, 'admin-a');
    await page.getByRole('link', { name: 'Projects' }).click();
    await page.getByRole('button', { name: '+ New project' }).click();
    const form = page.getByRole('form', { name: 'New project' });
    await form.getByLabel('Key').fill(projectKey);
    await form.getByLabel('Name', { exact: true }).fill('Tracker flow');
    await form.getByLabel('Item type name').fill('Task');
    await form.getByLabel('Workflow').selectOption('trackerFlow');
    await form.getByRole('button', { name: 'Save' }).click();
    await expect(page.getByRole('cell', { name: projectKey, exact: true })).toBeVisible();
  });

  test('an admin renames the project and adds an item type', async ({ page }) => {
    await login(page, 'admin-a');
    await page.getByRole('link', { name: 'Projects' }).click();
    await page.getByRole('button', { name: `Edit ${projectKey}` }).click();
    const form = page.getByRole('form', { name: `Edit project ${projectKey}` });
    await expect(form.getByLabel('Key')).toBeDisabled();

    await form.getByLabel('Name', { exact: true }).fill('Tracker flow renamed');
    await form.getByRole('button', { name: '+ Add item type' }).click();
    await form.getByLabel('Item type name').last().fill('Story');
    await form.getByLabel('Workflow').last().selectOption('trackerFlow');
    await form.getByRole('button', { name: 'Save' }).click();

    const row = page.getByRole('row', { name: new RegExp(projectKey) });
    await expect(row).toContainText('Tracker flow renamed');
    await expect(row).toContainText('Story -> trackerFlow');
  });

  test('a user creates an item, moves it across the board, closes and reopens it', async ({ page }) => {
    await login(page, 'user-a');
    await page.getByLabel('Project').selectOption(projectKey);

    await page.getByRole('button', { name: '+ Create' }).click();
    const dialog = page.getByRole('dialog', { name: 'Create item' });
    await dialog.getByLabel('Title').fill(title);
    await dialog.getByLabel('Assignee').fill('user-a');
    await dialog.getByRole('button', { name: 'Create' }).click();
    await expect(page.getByRole('heading', { name: title })).toBeVisible();

    const key = `${projectKey}-1`;
    await expect(page.locator('[data-element-id="open"].wfp-current-status')).toHaveCount(1);
    await page.getByLabel('Add a comment').fill('Picking this up');
    await page.getByRole('button', { name: 'Post' }).click();
    await expect(page.getByText('Picking this up')).toBeVisible();

    await page.getByRole('link', { name: 'Board' }).click();
    const card = page.getByTestId(`card-${key}`);
    await expect(column(page, 'Open').getByTestId(`card-${key}`)).toBeVisible();

    await card.dragTo(column(page, 'In progress'));
    await expect(column(page, 'In progress').getByTestId(`card-${key}`)).toContainText('Doing');

    await card.dragTo(column(page, 'Done'));
    await expect(column(page, 'Done').getByTestId(`card-${key}`)).toContainText('Done');

    await card.dragTo(column(page, 'Open'));
    await expect(column(page, 'Open').getByTestId(`card-${key}`)).toContainText('Open');
  });

  test('the audit log records the item', async ({ request }) => {
    const token = await adminToken(request);

    const audit = await request.get(`${WEB_URL}/api/audit?entityId=${projectKey}-1&eventType=item.transitioned`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    expect((await audit.json()).totalElements).toBe(3);
  });

  test('another tenant cannot see the project', async ({ page }) => {
    await login(page, 'user-b');

    await expect(page.getByLabel('Project').locator('option', { hasText: projectKey })).toHaveCount(0);
  });
});
