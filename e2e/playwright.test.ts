import { test, expect } from '@playwright/test';

test.describe('Workflow Platform E2E', () => {
  test('Keycloak realm exists', async ({ page }) => {
    const response = await page.goto('http://localhost:8180/realms/workflow-platform/.well-known/openid-configuration');
    expect(response?.status()).toBe(200);
    const body = await response?.json();
    expect(body.issuer).toContain('workflow-platform');
  });

  test('Tracker app loads and redirects to Keycloak', async ({ page }) => {
    await page.goto('http://localhost:5175/');
    // OIDC-protected app redirects to Keycloak login
    await expect(page).toHaveURL(/realms\/workflow-platform.*client_id=wfp-web/);
    await expect(page.getByRole('heading', { name: 'Sign in to your account' })).toBeVisible();
  });

  test('All backend services healthy', async ({ request }) => {
    for (const port of [8081]) {
      const response = await request.get(`http://localhost:${port}/actuator/health`);
      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.status).toBe('UP');
    }
  });
});
