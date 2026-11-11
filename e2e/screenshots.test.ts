import { test, expect, Page } from '@playwright/test';
import path from 'path';

const SCREENSHOTS_DIR = path.join(__dirname, '..', 'docs', 'screenshots');
const WEB_URL = 'http://localhost:5175';

async function login(page: Page, username: string) {
  await page.goto(WEB_URL);
  await page.getByRole('textbox', { name: 'Username or email' }).fill(username);
  await page.getByRole('button', { name: 'Sign In' }).click();
  await page.getByRole('textbox', { name: 'Password' }).fill('password');
  await page.getByRole('button', { name: 'Sign In' }).click();
  await expect(page.getByRole('heading', { name: 'My work' })).toBeVisible({ timeout: 20000 });
}

function capture(page: Page, name: string) {
  return page.screenshot({ path: path.join(SCREENSHOTS_DIR, `${name}.png`) });
}

test.describe('Screenshot Capture', () => {
  test.use({ viewport: { width: 1280, height: 720 } });

  test('Keycloak login page', async ({ page }) => {
    await page.goto(WEB_URL);
    await expect(page.getByRole('textbox', { name: 'Username or email' })).toBeVisible({ timeout: 15000 });
    await capture(page, 'keycloak-login');
  });

  test('User screens', async ({ page }) => {
    await login(page, 'user-a');
    await page.getByLabel('Project').selectOption('DEMO');
    await expect(page.getByRole('link', { name: 'DEMO-1', exact: true })).toBeVisible();
    await capture(page, 'my-work');

    await page.getByRole('link', { name: 'Items' }).click();
    await expect(page.locator('tbody tr').first()).toBeVisible();
    await page.getByRole('button', { name: '+ Add filter' }).click();
    await capture(page, 'items');

    await page.getByRole('button', { name: '+ Create' }).click();
    await expect(page.getByRole('dialog', { name: 'Create item' })).toBeVisible();
    await capture(page, 'create-item');
    await page.getByRole('button', { name: 'Cancel' }).click();

    await page.getByRole('link', { name: 'DEMO-1', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Comments' })).toBeVisible();
    await page.waitForTimeout(800);
    await capture(page, 'item-detail');

    await page.getByRole('link', { name: 'Board' }).click();
    await expect(page.getByRole('region', { name: 'Open', exact: true }).getByRole('link').first()).toBeVisible();
    await capture(page, 'board');
  });

  test('Admin screens', async ({ page }) => {
    await login(page, 'admin-a');
    await page.getByLabel('Project').selectOption('DEMO');

    await page.getByRole('link', { name: 'Projects' }).click();
    await expect(page.getByRole('cell', { name: 'DEMO', exact: true })).toBeVisible();
    await capture(page, 'admin-projects');

    await page.getByRole('link', { name: 'Workflows' }).click();
    const workflow = page.getByRole('row', { name: /trackerFlow/ });
    await expect(workflow).toBeVisible();
    await capture(page, 'admin-workflows');

    await workflow.getByRole('link', { name: 'Edit' }).click();
    await expect(page.locator('.djs-container svg').first()).toBeVisible({ timeout: 20000 });
    await page.waitForTimeout(800);
    await capture(page, 'workflow-editor');

    await page.getByRole('link', { name: 'Fields' }).click();
    await expect(page.getByRole('cell', { name: 'Severity', exact: true })).toBeVisible();
    await capture(page, 'admin-fields');

    await page.getByRole('link', { name: 'Audit log' }).click();
    await expect(page.locator('tbody tr').first()).toBeVisible();
    await capture(page, 'admin-audit-log');
  });
});
