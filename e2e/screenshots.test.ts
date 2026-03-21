import { test, expect, Page } from '@playwright/test';
import path from 'path';

const SCREENSHOTS_DIR = path.join(__dirname, '..', 'docs', 'screenshots');

async function loginViaRoot(page: Page, port: number, sidebarTitle: string) {
  // Always start from root to avoid OIDC redirect issues with deep links
  await page.goto(`http://localhost:${port}/`);
  await page.getByRole('textbox', { name: 'Username or email' }).fill('admin-a');
  await page.getByRole('textbox', { name: 'Password' }).fill('password');
  await page.getByRole('button', { name: 'Sign In' }).click();
  // Wait for app shell sidebar to render (auth complete)
  await expect(page.getByText(sidebarTitle)).toBeVisible({ timeout: 20000 });
}

test.describe('Screenshot Capture', () => {
  test.describe.configure({ timeout: 45000 });

  test('Keycloak login page', async ({ page }) => {
    await page.goto('http://localhost:5173/');
    await expect(page.getByRole('heading', { name: 'Sign in to your account' })).toBeVisible();
    await page.screenshot({ path: path.join(SCREENSHOTS_DIR, 'keycloak-login.png'), fullPage: true });
  });

  test('Admin Portal — Dashboard', async ({ page }) => {
    await loginViaRoot(page, 5173, 'WFP Admin');
    // Already on dashboard after login redirect
    await expect(page.getByText('Deployed Processes')).toBeVisible({ timeout: 10000 });
    await page.screenshot({ path: path.join(SCREENSHOTS_DIR, 'admin-dashboard.png'), fullPage: true });
  });

  test('Admin Portal — Process Designer', async ({ page }) => {
    await loginViaRoot(page, 5173, 'WFP Admin');
    await page.getByRole('link', { name: 'Processes' }).click();
    await page.getByText('New Process').click();
    await expect(page.locator('.bjs-container')).toBeVisible({ timeout: 10000 });
    await page.waitForTimeout(1000);
    await page.screenshot({ path: path.join(SCREENSHOTS_DIR, 'admin-process-designer.png'), fullPage: true });
  });

  test('Admin Portal — Process List', async ({ page }) => {
    await loginViaRoot(page, 5173, 'WFP Admin');
    await page.getByRole('link', { name: 'Processes' }).click();
    await expect(page.getByText('Process Definitions')).toBeVisible({ timeout: 10000 });
    await page.waitForTimeout(500);
    await page.screenshot({ path: path.join(SCREENSHOTS_DIR, 'admin-process-list.png'), fullPage: true });
  });

  test('Admin Portal — Custom Fields', async ({ page }) => {
    await loginViaRoot(page, 5173, 'WFP Admin');
    await page.getByText('Custom Fields').click();
    await expect(page.getByText('Custom Field Editor')).toBeVisible({ timeout: 10000 });
    await page.waitForTimeout(500);
    await page.screenshot({ path: path.join(SCREENSHOTS_DIR, 'admin-custom-fields.png'), fullPage: true });
  });

  test('Admin Portal — Audit Log', async ({ page }) => {
    await loginViaRoot(page, 5173, 'WFP Admin');
    await page.getByText('Audit Log').click();
    await expect(page.getByRole('heading', { name: 'Audit Log' })).toBeVisible({ timeout: 10000 });
    await page.waitForTimeout(1000); // let data load
    await page.screenshot({ path: path.join(SCREENSHOTS_DIR, 'admin-audit-log.png'), fullPage: true });
  });

  test('User Portal — Dashboard', async ({ page }) => {
    await loginViaRoot(page, 5174, 'WFP Portal');
    await expect(page.getByText('Welcome')).toBeVisible({ timeout: 10000 });
    await page.waitForTimeout(500);
    await page.screenshot({ path: path.join(SCREENSHOTS_DIR, 'user-dashboard.png'), fullPage: true });
  });

  test('User Portal — Task Inbox', async ({ page }) => {
    await loginViaRoot(page, 5174, 'WFP Portal');
    await page.getByText('My Tasks').first().click();
    await expect(page.getByRole('heading', { name: 'My Tasks' })).toBeVisible({ timeout: 10000 });
    await page.waitForTimeout(500);
    await page.screenshot({ path: path.join(SCREENSHOTS_DIR, 'user-task-inbox.png'), fullPage: true });
  });

  test('User Portal — Start Process', async ({ page }) => {
    await loginViaRoot(page, 5174, 'WFP Portal');
    await page.getByRole('link', { name: 'Start Process' }).click();
    await expect(page.getByRole('heading', { name: 'Start Process' })).toBeVisible({ timeout: 10000 });
    await page.waitForTimeout(500);
    await page.screenshot({ path: path.join(SCREENSHOTS_DIR, 'user-start-process.png'), fullPage: true });
  });

  test('User Portal — Notifications', async ({ page }) => {
    await loginViaRoot(page, 5174, 'WFP Portal');
    await page.getByRole('link', { name: 'Notifications' }).click();
    await expect(page.getByRole('heading', { name: 'Notifications' })).toBeVisible({ timeout: 10000 });
    await page.waitForTimeout(500);
    await page.screenshot({ path: path.join(SCREENSHOTS_DIR, 'user-notifications.png'), fullPage: true });
  });

  test('RabbitMQ Management — Overview', async ({ page }) => {
    await page.goto('http://localhost:15672/');
    await page.getByRole('textbox').first().fill('wfp');
    await page.getByRole('textbox').nth(1).fill('wfp_secret');
    await page.getByRole('button', { name: 'Login' }).click();
    await expect(page.getByRole('heading', { name: 'Overview' })).toBeVisible({ timeout: 10000 });
    await page.screenshot({ path: path.join(SCREENSHOTS_DIR, 'rabbitmq-overview.png'), fullPage: true });
  });

  test('Gateway Health', async ({ page }) => {
    await page.goto('http://localhost:9080/actuator/health');
    await page.screenshot({ path: path.join(SCREENSHOTS_DIR, 'gateway-health.png') });
  });
});
