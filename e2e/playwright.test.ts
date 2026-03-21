import { test, expect } from '@playwright/test';

test.describe('Workflow Platform E2E', () => {
  test('Keycloak realm exists', async ({ page }) => {
    const response = await page.goto('http://localhost:8180/realms/workflow-platform/.well-known/openid-configuration');
    expect(response?.status()).toBe(200);
    const body = await response?.json();
    expect(body.issuer).toContain('workflow-platform');
  });

  test('RabbitMQ management accessible', async ({ page }) => {
    await page.goto('http://localhost:15672/');
    await page.getByRole('textbox').first().fill('wfp');
    await page.getByRole('textbox').nth(1).fill('wfp_secret');
    await page.getByRole('button', { name: 'Login' }).click();
    await expect(page.getByRole('heading', { name: 'Overview' })).toBeVisible({ timeout: 10000 });
  });

  test('Admin Portal loads and redirects to Keycloak', async ({ page }) => {
    await page.goto('http://localhost:5173/');
    // OIDC-protected app redirects to Keycloak login
    await expect(page).toHaveURL(/realms\/workflow-platform.*client_id=wfp-admin-portal/);
    await expect(page.getByRole('heading', { name: 'Sign in to your account' })).toBeVisible();
  });

  test('User Portal loads and redirects to Keycloak', async ({ page }) => {
    await page.goto('http://localhost:5174/');
    // OIDC-protected app redirects to Keycloak login
    await expect(page).toHaveURL(/realms\/workflow-platform.*client_id=wfp-user-portal/);
    await expect(page.getByRole('heading', { name: 'Sign in to your account' })).toBeVisible();
  });

  test('Gateway health check', async ({ request }) => {
    const response = await request.get('http://localhost:9080/actuator/health');
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.status).toBe('UP');
  });

  test('All backend services healthy', async ({ request }) => {
    for (const port of [8081, 8082, 8083, 8084]) {
      const response = await request.get(`http://localhost:${port}/actuator/health`);
      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.status).toBe('UP');
    }
  });

  test('Full workflow E2E through gateway', async ({ request }) => {
    // Get token
    const tokenResponse = await request.post('http://localhost:8180/realms/workflow-platform/protocol/openid-connect/token', {
      form: {
        grant_type: 'password',
        client_id: 'wfp-admin-portal',
        username: 'admin-a',
        password: 'password',
      },
    });
    expect(tokenResponse.status()).toBe(200);
    const { access_token: token } = await tokenResponse.json();

    const headers = { Authorization: `Bearer ${token}` };

    // Deploy BPMN
    const deployResponse = await request.post('http://localhost:9080/api/workflow/deployments', {
      headers,
      data: {
        name: 'Playwright Test',
        bpmnXml: `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://wfp.com/processes">
  <process id="playwrightTest" name="Playwright Test" isExecutable="true">
    <startEvent id="start"/>
    <userTask id="task1" name="Test Task" flowable:assignee="\${initiator}"/>
    <endEvent id="end"/>
    <sequenceFlow sourceRef="start" targetRef="task1"/>
    <sequenceFlow sourceRef="task1" targetRef="end"/>
  </process>
</definitions>`,
      },
    });
    expect(deployResponse.status()).toBe(201);
    const deployment = await deployResponse.json();
    expect(deployment.deploymentId).toBeTruthy();

    // Start process
    const startResponse = await request.post('http://localhost:9080/api/workflow/processes', {
      headers,
      data: {
        processDefinitionKey: 'playwrightTest',
        variables: { initiator: 'admin-a' },
      },
    });
    expect(startResponse.status()).toBe(201);

    // List tasks
    const tasksResponse = await request.get('http://localhost:9080/api/workflow/tasks?assignee=admin-a', { headers });
    expect(tasksResponse.status()).toBe(200);
    const tasks = await tasksResponse.json();
    const task = tasks.content.find((t: any) => t.taskDefinitionKey === 'task1');
    expect(task).toBeTruthy();

    // Complete task
    const completeResponse = await request.post(`http://localhost:9080/api/workflow/tasks/${task.id}/complete`, {
      headers,
      data: {},
    });
    expect(completeResponse.status()).toBe(204);

    // Check audit trail
    const auditResponse = await request.get('http://localhost:9080/api/audit', { headers });
    expect(auditResponse.status()).toBe(200);
    const audit = await auditResponse.json();
    expect(audit.totalElements).toBeGreaterThan(0);

    // Check notifications
    const notifResponse = await request.get('http://localhost:9080/api/notifications', { headers });
    expect(notifResponse.status()).toBe(200);
  });

  test('Multi-tenant isolation', async ({ request }) => {
    // Get tokens for both tenants
    const getToken = async (username: string) => {
      const response = await request.post('http://localhost:8180/realms/workflow-platform/protocol/openid-connect/token', {
        form: { grant_type: 'password', client_id: 'wfp-admin-portal', username, password: 'password' },
      });
      return (await response.json()).access_token;
    };

    const tokenA = await getToken('admin-a');
    const tokenB = await getToken('admin-b');

    // Tenant A lists processes
    const processesA = await request.get('http://localhost:9080/api/workflow/processes', {
      headers: { Authorization: `Bearer ${tokenA}` },
    });
    const dataA = await processesA.json();

    // Tenant B lists processes — should see different (or no) data
    const processesB = await request.get('http://localhost:9080/api/workflow/processes', {
      headers: { Authorization: `Bearer ${tokenB}` },
    });
    const dataB = await processesB.json();

    // Tenant B should not see Tenant A's processes
    for (const proc of dataB.content) {
      expect(proc.tenantId).toBe('tenant-b');
    }
    for (const proc of dataA.content) {
      expect(proc.tenantId).toBe('tenant-a');
    }
  });
});
