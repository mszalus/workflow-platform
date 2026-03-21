import { test, expect, type APIRequestContext } from '@playwright/test';

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const KEYCLOAK_URL = 'http://localhost:8180';
const GATEWAY_URL = 'http://localhost:9080';
const ADMIN_PORTAL_URL = 'http://localhost:5173';
const USER_PORTAL_URL = 'http://localhost:5174';
const RABBITMQ_URL = 'http://localhost:15672';
const TOKEN_ENDPOINT = `${KEYCLOAK_URL}/realms/workflow-platform/protocol/openid-connect/token`;
const OIDC_DISCOVERY = `${KEYCLOAK_URL}/realms/workflow-platform/.well-known/openid-configuration`;

const USERS = {
  'admin-a': { username: 'admin-a', password: 'password', tenant: 'tenant-a' },
  'user-a': { username: 'user-a', password: 'password', tenant: 'tenant-a' },
  'admin-b': { username: 'admin-b', password: 'password', tenant: 'tenant-b' },
  'user-b': { username: 'user-b', password: 'password', tenant: 'tenant-b' },
} as const;

/** Navigate to a portal URL and handle Keycloak login if redirected */
async function loginIfNeeded(page: import('@playwright/test').Page, portalUrl: string, username: string, password: string) {
  await page.goto(portalUrl);
  if (page.url().includes('realms/workflow-platform')) {
    await page.getByLabel('Username or email').fill(username);
    await page.getByLabel('Password', { exact: true }).fill(password);
    await page.getByRole('button', { name: 'Sign In' }).click();
  }
  await page.waitForURL(/dashboard/, { timeout: 20000 });
}

// ---------------------------------------------------------------------------
// BPMN XML templates
// ---------------------------------------------------------------------------

const SIMPLE_PROCESS_BPMN = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://wfp.com/processes">
  <process id="comprehensiveSimple" name="Comprehensive Simple Process" isExecutable="true">
    <startEvent id="start" flowable:initiator="initiator"/>
    <userTask id="simpleTask" name="Simple Task" flowable:assignee="\${initiator}"/>
    <endEvent id="end"/>
    <sequenceFlow sourceRef="start" targetRef="simpleTask"/>
    <sequenceFlow sourceRef="simpleTask" targetRef="end"/>
  </process>
</definitions>`;

const TENANT_ISOLATION_BPMN = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://wfp.com/processes">
  <process id="tenantIsolationTest" name="Tenant Isolation Test" isExecutable="true">
    <startEvent id="start" flowable:initiator="initiator"/>
    <userTask id="isolationTask" name="Isolation Task" flowable:assignee="\${initiator}"/>
    <endEvent id="end"/>
    <sequenceFlow sourceRef="start" targetRef="isolationTask"/>
    <sequenceFlow sourceRef="isolationTask" targetRef="end"/>
  </process>
</definitions>`;

const FLOWABLE_PROPERTIES_BPMN = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://wfp.com/processes">
  <process id="flowablePropsTest" name="Flowable Properties Test" isExecutable="true">
    <startEvent id="start" flowable:initiator="initiator"/>
    <userTask id="propsTask" name="Properties Task"
              flowable:assignee="\${initiator}"
              flowable:candidateGroups="managers,reviewers"
              flowable:formKey="taskForm"
              flowable:priority="75"/>
    <serviceTask id="serviceStep" name="Service Step"
                 flowable:class="com.wfp.workflow.delegate.NoOpDelegate"
                 flowable:async="true"/>
    <endEvent id="end"/>
    <sequenceFlow sourceRef="start" targetRef="propsTask"/>
    <sequenceFlow sourceRef="propsTask" targetRef="serviceStep"/>
    <sequenceFlow sourceRef="serviceStep" targetRef="end"/>
  </process>
</definitions>`;

// ---------------------------------------------------------------------------
// Helper: get JWT token from Keycloak
// ---------------------------------------------------------------------------

const tokenCache: Record<string, { token: string; expiry: number }> = {};

async function getToken(
  request: APIRequestContext,
  username: string,
  clientId = 'wfp-admin-portal',
): Promise<string> {
  const cacheKey = `${username}:${clientId}`;
  const cached = tokenCache[cacheKey];
  if (cached && cached.expiry > Date.now()) {
    return cached.token;
  }

  const response = await request.post(TOKEN_ENDPOINT, {
    form: {
      grant_type: 'password',
      client_id: clientId,
      username,
      password: 'password',
    },
  });
  expect(response.status()).toBe(200);

  const body = await response.json();
  expect(body.access_token).toBeTruthy();

  // Cache with 4-minute expiry (tokens are typically 5min)
  tokenCache[cacheKey] = {
    token: body.access_token,
    expiry: Date.now() + 4 * 60 * 1000,
  };

  return body.access_token;
}

function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}` };
}

function decodeJwtPayload(token: string): Record<string, unknown> {
  const parts = token.split('.');
  const payload = Buffer.from(parts[1], 'base64url').toString('utf-8');
  return JSON.parse(payload);
}

// Small helper to wait for async event propagation (RabbitMQ -> services)
async function waitForEventPropagation(ms = 2000) {
  await new Promise((resolve) => setTimeout(resolve, ms));
}

// ==========================================================================
// 1. Infrastructure Tests
// ==========================================================================

test.describe('1. Infrastructure Tests', () => {
  test('Keycloak OIDC discovery endpoint responds', async ({ request }) => {
    const response = await request.get(OIDC_DISCOVERY);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.issuer).toContain('workflow-platform');
    expect(body.authorization_endpoint).toBeTruthy();
    expect(body.token_endpoint).toBeTruthy();
    expect(body.jwks_uri).toBeTruthy();
  });

  test('RabbitMQ management UI accessible', async ({ page }) => {
    await page.goto(RABBITMQ_URL);
    // Fill login form
    await page.getByRole('textbox').first().fill('wfp');
    await page.getByRole('textbox').nth(1).fill('wfp_secret');
    await page.getByRole('button', { name: 'Login' }).click();
    await expect(page.getByRole('heading', { name: 'Overview' })).toBeVisible({ timeout: 10000 });
  });

  test('Gateway health check returns UP', async ({ request }) => {
    const response = await request.get(`${GATEWAY_URL}/actuator/health`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.status).toBe('UP');
  });

  test('All backend services healthy (ports 8081-8084)', async ({ request }) => {
    const services = [
      { port: 8081, name: 'workflow-service' },
      { port: 8082, name: 'custom-fields-service' },
      { port: 8083, name: 'notification-service' },
      { port: 8084, name: 'audit-service' },
    ];

    for (const svc of services) {
      const response = await request.get(`http://localhost:${svc.port}/actuator/health`);
      expect(response.status(), `${svc.name} health check failed`).toBe(200);
      const body = await response.json();
      expect(body.status, `${svc.name} status is not UP`).toBe('UP');
    }
  });
});

// ==========================================================================
// 2. Authentication Tests
// ==========================================================================

test.describe('2. Authentication Tests', () => {
  test('Get JWT token for each user', async ({ request }) => {
    for (const [name, user] of Object.entries(USERS)) {
      const token = await getToken(request, user.username);
      expect(token, `Failed to get token for ${name}`).toBeTruthy();
      expect(typeof token).toBe('string');
      expect(token.split('.')).toHaveLength(3); // JWT has 3 parts
    }
  });

  test('JWT contains expected claims (preferred_username, tenant_id)', async ({ request }) => {
    for (const [name, user] of Object.entries(USERS)) {
      const token = await getToken(request, user.username);
      const payload = decodeJwtPayload(token);

      expect(payload.preferred_username, `${name}: missing preferred_username`).toBe(user.username);
      expect(payload.tenant_id, `${name}: missing tenant_id`).toBe(user.tenant);
    }
  });

  test('Token works against gateway API', async ({ request }) => {
    const token = await getToken(request, 'admin-a');
    const response = await request.get(`${GATEWAY_URL}/api/workflow/deployments`, {
      headers: authHeaders(token),
    });
    expect(response.status()).toBe(200);
  });
});

// ==========================================================================
// 3. Process Lifecycle Tests (as admin-a, tenant-a)
// ==========================================================================

test.describe.serial('3. Process Lifecycle Tests', () => {
  let token: string;
  let deploymentId: string;
  let processDefinitionId: string;
  let processInstanceId: string;
  let taskId: string;

  test('Get token for admin-a', async ({ request }) => {
    token = await getToken(request, 'admin-a');
    expect(token).toBeTruthy();
  });

  test('Deploy a test BPMN process', async ({ request }) => {
    const response = await request.post(`${GATEWAY_URL}/api/workflow/deployments`, {
      headers: authHeaders(token),
      data: {
        name: 'Comprehensive Simple Process',
        bpmnXml: SIMPLE_PROCESS_BPMN,
      },
    });
    expect(response.status()).toBe(201);
    const body = await response.json();
    expect(body.deploymentId).toBeTruthy();
    expect(body.name).toBe('Comprehensive Simple Process');
    deploymentId = body.deploymentId;
  });

  test('List process definitions — verify new process exists', async ({ request }) => {
    const response = await request.get(`${GATEWAY_URL}/api/workflow/deployments`, {
      headers: authHeaders(token),
    });
    expect(response.status()).toBe(200);
    const definitions = await response.json();
    expect(Array.isArray(definitions)).toBe(true);

    const found = definitions.find((pd: any) => pd.key === 'comprehensiveSimple');
    expect(found, 'comprehensiveSimple not found in definitions').toBeTruthy();
    expect(found.deploymentId).toBe(deploymentId);
    processDefinitionId = found.id;
  });

  test('Start a process instance', async ({ request }) => {
    const response = await request.post(`${GATEWAY_URL}/api/workflow/processes`, {
      headers: authHeaders(token),
      data: {
        processDefinitionKey: 'comprehensiveSimple',
        businessKey: 'lifecycle-test-001',
      },
    });
    expect(response.status()).toBe(201);
    const body = await response.json();
    expect(body.id).toBeTruthy();
    expect(body.processDefinitionKey).toBe('comprehensiveSimple');
    processInstanceId = body.id;
  });

  test('List process instances — verify new instance appears', async ({ request }) => {
    const response = await request.get(`${GATEWAY_URL}/api/workflow/processes`, {
      headers: authHeaders(token),
    });
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.content).toBeTruthy();

    const found = body.content.find((p: any) => p.id === processInstanceId);
    expect(found, 'Process instance not found in list').toBeTruthy();
    expect(found.businessKey).toBe('lifecycle-test-001');
  });

  test('List tasks assigned to admin-a — verify task exists', async ({ request }) => {
    const response = await request.get(`${GATEWAY_URL}/api/workflow/tasks?assignee=admin-a`, {
      headers: authHeaders(token),
    });
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.content).toBeTruthy();

    const found = body.content.find(
      (t: any) => t.processInstanceId === processInstanceId && t.taskDefinitionKey === 'simpleTask',
    );
    expect(found, 'Task for comprehensiveSimple process not found').toBeTruthy();
    taskId = found.id;
  });

  test('Get task detail — verify task has correct name and assignee', async ({ request }) => {
    const response = await request.get(`${GATEWAY_URL}/api/workflow/tasks/${taskId}`, {
      headers: authHeaders(token),
    });
    expect(response.status()).toBe(200);
    const task = await response.json();
    expect(task.name).toBe('Simple Task');
    expect(task.assignee).toBe('admin-a');
    expect(task.processInstanceId).toBe(processInstanceId);
    expect(task.taskDefinitionKey).toBe('simpleTask');
  });

  test('Complete task — verify 204', async ({ request }) => {
    const response = await request.post(`${GATEWAY_URL}/api/workflow/tasks/${taskId}/complete`, {
      headers: authHeaders(token),
      data: {},
    });
    expect(response.status()).toBe(204);
  });

  test('Verify process completes (no more tasks for this process instance)', async ({ request }) => {
    // Give the engine a moment to finalize
    await waitForEventPropagation(1000);

    const response = await request.get(`${GATEWAY_URL}/api/workflow/tasks?assignee=admin-a`, {
      headers: authHeaders(token),
    });
    expect(response.status()).toBe(200);
    const body = await response.json();

    const remainingTasks = body.content.filter(
      (t: any) => t.processInstanceId === processInstanceId,
    );
    expect(remainingTasks).toHaveLength(0);
  });
});

// ==========================================================================
// 4. Approval Flow Tests (user-a + admin-a, tenant-a)
// ==========================================================================

test.describe.serial('4. Approval Flow Tests', () => {
  let userToken: string;
  let adminToken: string;
  let processInstanceId: string;
  let submitTaskId: string;
  let approvalTaskId: string;

  test('Get tokens for user-a and admin-a', async ({ request }) => {
    userToken = await getToken(request, 'user-a');
    adminToken = await getToken(request, 'admin-a');
    expect(userToken).toBeTruthy();
    expect(adminToken).toBeTruthy();
  });

  test('Deploy approval process if not already present', async ({ request }) => {
    // Check if approvalProcess is already deployed (auto-deployed from resources)
    const listResponse = await request.get(`${GATEWAY_URL}/api/workflow/deployments`, {
      headers: authHeaders(adminToken),
    });
    const definitions = await listResponse.json();
    const existing = definitions.find((pd: any) => pd.key === 'approvalProcess');

    if (!existing) {
      // Deploy it manually
      const approvalBpmn = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://wfp.com/processes">
  <process id="approvalProcess" name="Approval Process" isExecutable="true">
    <startEvent id="start" name="Start" flowable:initiator="initiator"/>
    <userTask id="submitRequest" name="Submit Request" flowable:assignee="\${initiator}"/>
    <userTask id="managerApproval" name="Manager Approval" flowable:candidateGroups="managers"/>
    <exclusiveGateway id="approvalDecision" name="Approved?"/>
    <endEvent id="approvedEnd" name="Approved"/>
    <endEvent id="rejectedEnd" name="Rejected"/>
    <sequenceFlow sourceRef="start" targetRef="submitRequest"/>
    <sequenceFlow sourceRef="submitRequest" targetRef="managerApproval"/>
    <sequenceFlow sourceRef="managerApproval" targetRef="approvalDecision"/>
    <sequenceFlow sourceRef="approvalDecision" targetRef="approvedEnd">
      <conditionExpression>\${approved == true}</conditionExpression>
    </sequenceFlow>
    <sequenceFlow sourceRef="approvalDecision" targetRef="rejectedEnd">
      <conditionExpression>\${approved == false}</conditionExpression>
    </sequenceFlow>
  </process>
</definitions>`;
      const deployResponse = await request.post(`${GATEWAY_URL}/api/workflow/deployments`, {
        headers: authHeaders(adminToken),
        data: { name: 'Approval Process', bpmnXml: approvalBpmn },
      });
      expect(deployResponse.status()).toBe(201);
    }
  });

  test('Start approval process as user-a', async ({ request }) => {
    const response = await request.post(`${GATEWAY_URL}/api/workflow/processes`, {
      headers: authHeaders(userToken),
      data: {
        processDefinitionKey: 'approvalProcess',
        businessKey: 'approval-test-001',
      },
    });
    expect(response.status()).toBe(201);
    const body = await response.json();
    expect(body.id).toBeTruthy();
    processInstanceId = body.id;
  });

  test('Verify "Submit Request" task assigned to user-a', async ({ request }) => {
    const response = await request.get(`${GATEWAY_URL}/api/workflow/tasks?assignee=user-a`, {
      headers: authHeaders(userToken),
    });
    expect(response.status()).toBe(200);
    const body = await response.json();

    const submitTask = body.content.find(
      (t: any) => t.processInstanceId === processInstanceId && t.taskDefinitionKey === 'submitRequest',
    );
    expect(submitTask, 'Submit Request task not found for user-a').toBeTruthy();
    expect(submitTask.name).toBe('Submit Request');
    expect(submitTask.assignee).toBe('user-a');
    submitTaskId = submitTask.id;
  });

  test('Complete "Submit Request"', async ({ request }) => {
    const response = await request.post(
      `${GATEWAY_URL}/api/workflow/tasks/${submitTaskId}/complete`,
      {
        headers: authHeaders(userToken),
        data: {},
      },
    );
    expect(response.status()).toBe(204);
  });

  test('Verify "Manager Approval" task exists (unassigned, candidateGroup=managers)', async ({ request }) => {
    // Small delay for engine to advance to next task
    await waitForEventPropagation(1000);

    // Query tasks by candidateGroup — the task should be unassigned
    const response = await request.get(
      `${GATEWAY_URL}/api/workflow/tasks?candidateGroup=managers`,
      { headers: authHeaders(adminToken) },
    );
    expect(response.status()).toBe(200);
    const body = await response.json();

    const approvalTask = body.content.find(
      (t: any) => t.processInstanceId === processInstanceId && t.taskDefinitionKey === 'managerApproval',
    );
    expect(approvalTask, 'Manager Approval task not found').toBeTruthy();
    expect(approvalTask.name).toBe('Manager Approval');
    // Task should not have an assignee yet (it is a candidate group task)
    expect(approvalTask.assignee).toBeFalsy();
    approvalTaskId = approvalTask.id;
  });

  test('Claim "Manager Approval" as admin-a', async ({ request }) => {
    const response = await request.post(
      `${GATEWAY_URL}/api/workflow/tasks/${approvalTaskId}/claim`,
      { headers: authHeaders(adminToken) },
    );
    expect(response.status()).toBe(204);

    // Verify it is now assigned to admin-a
    const detailResponse = await request.get(
      `${GATEWAY_URL}/api/workflow/tasks/${approvalTaskId}`,
      { headers: authHeaders(adminToken) },
    );
    expect(detailResponse.status()).toBe(200);
    const task = await detailResponse.json();
    expect(task.assignee).toBe('admin-a');
  });

  test('Complete "Manager Approval" with approved=true', async ({ request }) => {
    const response = await request.post(
      `${GATEWAY_URL}/api/workflow/tasks/${approvalTaskId}/complete`,
      {
        headers: authHeaders(adminToken),
        data: { variables: { approved: true } },
      },
    );
    expect(response.status()).toBe(204);
  });

  test('Verify process completed (no remaining tasks)', async ({ request }) => {
    await waitForEventPropagation(1000);

    // Check that no tasks remain for this process instance
    const tasksResponse = await request.get(`${GATEWAY_URL}/api/workflow/tasks?assignee=user-a`, {
      headers: authHeaders(userToken),
    });
    const tasks = await tasksResponse.json();
    const remaining = tasks.content.filter((t: any) => t.processInstanceId === processInstanceId);
    expect(remaining).toHaveLength(0);

    // Also verify via history that the process is completed
    const historyResponse = await request.get(
      `${GATEWAY_URL}/api/workflow/history/processes`,
      { headers: authHeaders(adminToken) },
    );
    if (historyResponse.status() === 200) {
      const history = await historyResponse.json();
      const completed = history.content?.find((p: any) => p.id === processInstanceId);
      if (completed) {
        expect(completed.endTime).toBeTruthy();
      }
    }
  });
});

// ==========================================================================
// 5. Comments Tests
// ==========================================================================

test.describe.serial('5. Comments Tests', () => {
  let token: string;
  let processInstanceId: string;

  test('Setup: start a process to comment on', async ({ request }) => {
    token = await getToken(request, 'admin-a');

    // Start a process
    const response = await request.post(`${GATEWAY_URL}/api/workflow/processes`, {
      headers: authHeaders(token),
      data: {
        processDefinitionKey: 'comprehensiveSimple',
        businessKey: 'comment-test-001',
      },
    });
    expect(response.status()).toBe(201);
    processInstanceId = (await response.json()).id;
  });

  test('Add comment to process instance', async ({ request }) => {
    const response = await request.post(
      `${GATEWAY_URL}/api/workflow/processes/${processInstanceId}/comments`,
      {
        headers: authHeaders(token),
        data: { content: 'This is a test comment from comprehensive E2E' },
      },
    );
    expect(response.status()).toBe(201);
    const comment = await response.json();
    expect(comment.content).toBe('This is a test comment from comprehensive E2E');
    expect(comment.userId).toBe('admin-a');
    expect(comment.processInstanceId).toBe(processInstanceId);
  });

  test('List comments — verify comment exists', async ({ request }) => {
    const response = await request.get(
      `${GATEWAY_URL}/api/workflow/processes/${processInstanceId}/comments`,
      { headers: authHeaders(token) },
    );
    expect(response.status()).toBe(200);
    const comments = await response.json();
    expect(Array.isArray(comments)).toBe(true);
    expect(comments.length).toBeGreaterThanOrEqual(1);

    const found = comments.find((c: any) => c.content === 'This is a test comment from comprehensive E2E');
    expect(found).toBeTruthy();
  });

  test('Cleanup: complete the task to avoid dangling processes', async ({ request }) => {
    const tasksResponse = await request.get(
      `${GATEWAY_URL}/api/workflow/tasks?assignee=admin-a`,
      { headers: authHeaders(token) },
    );
    const tasks = await tasksResponse.json();
    const task = tasks.content.find((t: any) => t.processInstanceId === processInstanceId);
    if (task) {
      await request.post(`${GATEWAY_URL}/api/workflow/tasks/${task.id}/complete`, {
        headers: authHeaders(token),
        data: {},
      });
    }
  });
});

// ==========================================================================
// 6. Custom Fields Tests (as admin-a)
// ==========================================================================

test.describe.serial('6. Custom Fields Tests', () => {
  let token: string;
  let schemaId: string;

  test('Get admin-a token', async ({ request }) => {
    token = await getToken(request, 'admin-a');
  });

  test('Create a field schema', async ({ request }) => {
    const response = await request.post(`${GATEWAY_URL}/api/fields/schemas`, {
      headers: authHeaders(token),
      data: {
        processDefinitionKey: 'comprehensiveSimple',
        fieldKey: 'e2e_priority_level',
        label: 'Priority Level',
        fieldType: 'DROPDOWN',
        required: true,
        sortOrder: 1,
        placeholder: 'Select priority',
        options: [
          { label: 'Low', value: 'low', sortOrder: 1 },
          { label: 'Medium', value: 'medium', sortOrder: 2 },
          { label: 'High', value: 'high', sortOrder: 3 },
        ],
      },
    });
    expect(response.status()).toBe(201);
    const schema = await response.json();
    expect(schema.id).toBeTruthy();
    expect(schema.fieldKey).toBe('e2e_priority_level');
    expect(schema.label).toBe('Priority Level');
    expect(schema.fieldType).toBe('DROPDOWN');
    schemaId = schema.id;
  });

  test('List schemas by processDefinitionKey', async ({ request }) => {
    const response = await request.get(
      `${GATEWAY_URL}/api/fields/schemas?processDefinitionKey=comprehensiveSimple`,
      { headers: authHeaders(token) },
    );
    expect(response.status()).toBe(200);
    const schemas = await response.json();
    expect(Array.isArray(schemas)).toBe(true);

    const found = schemas.find((s: any) => s.id === schemaId);
    expect(found, 'Created schema not found in list').toBeTruthy();
    expect(found.fieldKey).toBe('e2e_priority_level');
  });

  test('Delete schema', async ({ request }) => {
    const response = await request.delete(`${GATEWAY_URL}/api/fields/schemas/${schemaId}`, {
      headers: authHeaders(token),
    });
    expect(response.status()).toBe(204);

    // Verify it is gone
    const listResponse = await request.get(
      `${GATEWAY_URL}/api/fields/schemas?processDefinitionKey=comprehensiveSimple`,
      { headers: authHeaders(token) },
    );
    const schemas = await listResponse.json();
    const found = schemas.find((s: any) => s.id === schemaId);
    expect(found).toBeFalsy();
  });
});

// ==========================================================================
// 7. Notifications Tests (as user-a)
// ==========================================================================

test.describe.serial('7. Notifications Tests', () => {
  let token: string;

  test('Get user-a token', async ({ request }) => {
    token = await getToken(request, 'user-a');
  });

  test('Get unread count', async ({ request }) => {
    const response = await request.get(`${GATEWAY_URL}/api/notifications/unread-count`, {
      headers: authHeaders(token),
    });
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(typeof body.count).toBe('number');
    expect(body.count).toBeGreaterThanOrEqual(0);
  });

  test('List notifications', async ({ request }) => {
    const response = await request.get(`${GATEWAY_URL}/api/notifications`, {
      headers: authHeaders(token),
    });
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.content).toBeTruthy();
    expect(Array.isArray(body.content)).toBe(true);
  });

  test('Mark all read', async ({ request }) => {
    const response = await request.put(`${GATEWAY_URL}/api/notifications/mark-all-read`, {
      headers: authHeaders(token),
    });
    expect(response.status()).toBe(200);
  });

  test('Verify unread count is 0 after mark-all-read', async ({ request }) => {
    const response = await request.get(`${GATEWAY_URL}/api/notifications/unread-count`, {
      headers: authHeaders(token),
    });
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.count).toBe(0);
  });
});

// ==========================================================================
// 8. Audit Trail Tests
// ==========================================================================

test.describe('8. Audit Trail Tests', () => {
  test('List audit events and verify process/task events exist', async ({ request }) => {
    const token = await getToken(request, 'admin-a');

    const response = await request.get(`${GATEWAY_URL}/api/audit`, {
      headers: authHeaders(token),
    });
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.content).toBeTruthy();
    expect(body.totalElements).toBeGreaterThan(0);

    // Verify that some expected event types exist from the lifecycle tests
    const eventTypes = body.content.map((e: any) => e.eventType);
    // At least one of these event types should be present from prior tests
    const expectedTypes = ['process.started', 'task.created', 'task.completed', 'process.completed'];
    const foundAny = expectedTypes.some((et) => eventTypes.includes(et));
    expect(foundAny, `Expected at least one of ${expectedTypes.join(', ')} in audit log`).toBe(true);
  });

  test('Query audit events by eventType', async ({ request }) => {
    const token = await getToken(request, 'admin-a');

    const response = await request.get(`${GATEWAY_URL}/api/audit?eventType=process.started`, {
      headers: authHeaders(token),
    });
    expect(response.status()).toBe(200);
    const body = await response.json();
    // Every returned event should match the filter
    for (const entry of body.content) {
      expect(entry.eventType).toBe('process.started');
    }
  });

  test('Query audit events by userId', async ({ request }) => {
    const token = await getToken(request, 'admin-a');

    const response = await request.get(`${GATEWAY_URL}/api/audit?userId=admin-a`, {
      headers: authHeaders(token),
    });
    expect(response.status()).toBe(200);
    const body = await response.json();
    for (const entry of body.content) {
      expect(entry.userId).toBe('admin-a');
    }
  });
});

// ==========================================================================
// 9. Multi-Tenant Isolation Tests
// ==========================================================================

test.describe.serial('9. Multi-Tenant Isolation Tests', () => {
  let tokenAdminA: string;
  let tokenAdminB: string;
  let tokenUserA: string;
  let tokenUserB: string;
  let tenantADeploymentId: string;
  let tenantAProcessId: string;

  test('Get tokens for all users', async ({ request }) => {
    tokenAdminA = await getToken(request, 'admin-a');
    tokenAdminB = await getToken(request, 'admin-b');
    tokenUserA = await getToken(request, 'user-a');
    tokenUserB = await getToken(request, 'user-b');
  });

  test('Deploy a process as admin-a (tenant-a)', async ({ request }) => {
    const response = await request.post(`${GATEWAY_URL}/api/workflow/deployments`, {
      headers: authHeaders(tokenAdminA),
      data: {
        name: 'Tenant Isolation Test',
        bpmnXml: TENANT_ISOLATION_BPMN,
      },
    });
    expect(response.status()).toBe(201);
    tenantADeploymentId = (await response.json()).deploymentId;
  });

  test('Verify admin-b (tenant-b) cannot see tenant-a process definitions', async ({ request }) => {
    const response = await request.get(`${GATEWAY_URL}/api/workflow/deployments`, {
      headers: authHeaders(tokenAdminB),
    });
    expect(response.status()).toBe(200);
    const definitions = await response.json();

    // tenant-b should NOT see the tenantIsolationTest process from tenant-a
    const found = definitions.find((pd: any) => pd.key === 'tenantIsolationTest');
    expect(found, 'Tenant-b should NOT see tenant-a process definition').toBeFalsy();
  });

  test('Start process as user-a (tenant-a)', async ({ request }) => {
    const response = await request.post(`${GATEWAY_URL}/api/workflow/processes`, {
      headers: authHeaders(tokenUserA),
      data: {
        processDefinitionKey: 'tenantIsolationTest',
        businessKey: 'isolation-001',
      },
    });
    expect(response.status()).toBe(201);
    tenantAProcessId = (await response.json()).id;
  });

  test('Verify user-b (tenant-b) cannot see tenant-a tasks', async ({ request }) => {
    const tasksResponseB = await request.get(`${GATEWAY_URL}/api/workflow/tasks?assignee=user-b`, {
      headers: authHeaders(tokenUserB),
    });
    expect(tasksResponseB.status()).toBe(200);
    const tasksB = await tasksResponseB.json();

    // user-b should have no tasks from the tenantIsolationTest process
    const found = tasksB.content.find((t: any) => t.processInstanceId === tenantAProcessId);
    expect(found, 'Tenant-b user should NOT see tenant-a tasks').toBeFalsy();
  });

  test('Verify user-b cannot see tenant-a process instances', async ({ request }) => {
    const response = await request.get(`${GATEWAY_URL}/api/workflow/processes`, {
      headers: authHeaders(tokenUserB),
    });
    expect(response.status()).toBe(200);
    const body = await response.json();

    for (const proc of body.content) {
      expect(proc.tenantId, 'Tenant-b sees non-tenant-b process').toBe('tenant-b');
    }
  });

  test('Verify tenant-a only sees its own processes', async ({ request }) => {
    const response = await request.get(`${GATEWAY_URL}/api/workflow/processes`, {
      headers: authHeaders(tokenUserA),
    });
    expect(response.status()).toBe(200);
    const body = await response.json();

    for (const proc of body.content) {
      expect(proc.tenantId, 'Tenant-a sees non-tenant-a process').toBe('tenant-a');
    }
  });

  test('Cleanup: complete the isolation test task', async ({ request }) => {
    const tasksResponse = await request.get(`${GATEWAY_URL}/api/workflow/tasks?assignee=user-a`, {
      headers: authHeaders(tokenUserA),
    });
    const tasks = await tasksResponse.json();
    const task = tasks.content.find((t: any) => t.processInstanceId === tenantAProcessId);
    if (task) {
      await request.post(`${GATEWAY_URL}/api/workflow/tasks/${task.id}/complete`, {
        headers: authHeaders(tokenUserA),
        data: {},
      });
    }
  });
});

// ==========================================================================
// 10. Negative Tests
// ==========================================================================

test.describe('10. Negative Tests', () => {
  test('Start a non-existent process definition — expect 4xx or 5xx', async ({ request }) => {
    const token = await getToken(request, 'admin-a');
    const response = await request.post(`${GATEWAY_URL}/api/workflow/processes`, {
      headers: authHeaders(token),
      data: {
        processDefinitionKey: 'nonExistentProcess_xyz_123',
      },
    });
    // Should fail with 404 or 500 (Flowable throws ObjectNotFoundException)
    expect(response.status()).toBeGreaterThanOrEqual(400);
  });

  test('Complete a non-existent task — expect 404', async ({ request }) => {
    const token = await getToken(request, 'admin-a');
    const response = await request.post(
      `${GATEWAY_URL}/api/workflow/tasks/non-existent-task-id-12345/complete`,
      {
        headers: authHeaders(token),
        data: {},
      },
    );
    expect(response.status()).toBeGreaterThanOrEqual(400);
  });

  test('Access API without token — expect 401', async ({ request }) => {
    const response = await request.get(`${GATEWAY_URL}/api/workflow/deployments`);
    expect(response.status()).toBe(401);
  });

  test('Access with invalid/expired token — expect 401', async ({ request }) => {
    const response = await request.get(`${GATEWAY_URL}/api/workflow/deployments`, {
      headers: { Authorization: 'Bearer invalid.jwt.token_that_is_clearly_not_valid' },
    });
    expect(response.status()).toBe(401);
  });

  test('Access with malformed Authorization header — expect 401', async ({ request }) => {
    const response = await request.get(`${GATEWAY_URL}/api/workflow/deployments`, {
      headers: { Authorization: 'NotBearer sometoken' },
    });
    expect(response.status()).toBe(401);
  });

  test('Get non-existent task detail — expect 404', async ({ request }) => {
    const token = await getToken(request, 'admin-a');
    const response = await request.get(
      `${GATEWAY_URL}/api/workflow/tasks/00000000-0000-0000-0000-000000000000`,
      { headers: authHeaders(token) },
    );
    expect(response.status()).toBeGreaterThanOrEqual(400);
  });
});

// ==========================================================================
// 11. BPMN Import/Export Tests (as admin-a)
// ==========================================================================

test.describe.serial('11. BPMN Import/Export Tests', () => {
  let token: string;
  let processDefinitionId: string;
  let retrievedBpmnXml: string;
  let redeployedProcessDefinitionId: string;

  test('Get admin-a token', async ({ request }) => {
    token = await getToken(request, 'admin-a');
  });

  test('Deploy BPMN with Flowable properties', async ({ request }) => {
    const response = await request.post(`${GATEWAY_URL}/api/workflow/deployments`, {
      headers: authHeaders(token),
      data: {
        name: 'Flowable Properties Test',
        bpmnXml: FLOWABLE_PROPERTIES_BPMN,
      },
    });
    expect(response.status()).toBe(201);
    const body = await response.json();
    expect(body.deploymentId).toBeTruthy();

    // Get the process definition ID
    const listResponse = await request.get(`${GATEWAY_URL}/api/workflow/deployments`, {
      headers: authHeaders(token),
    });
    const definitions = await listResponse.json();
    const found = definitions.find(
      (pd: any) => pd.key === 'flowablePropsTest' && pd.deploymentId === body.deploymentId,
    );
    expect(found).toBeTruthy();
    processDefinitionId = found.id;
  });

  test('Retrieve BPMN XML', async ({ request }) => {
    const response = await request.get(
      `${GATEWAY_URL}/api/workflow/deployments/${processDefinitionId}/bpmn`,
      { headers: authHeaders(token) },
    );
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.bpmnXml).toBeTruthy();
    retrievedBpmnXml = body.bpmnXml;
  });

  test('Verify all flowable:* attributes are preserved in retrieved XML', async () => {
    // Check that the critical Flowable properties survived round-trip
    expect(retrievedBpmnXml).toContain('flowable:initiator="initiator"');
    expect(retrievedBpmnXml).toContain('flowable:assignee');
    expect(retrievedBpmnXml).toContain('flowable:candidateGroups');
    expect(retrievedBpmnXml).toContain('flowable:formKey="taskForm"');
    expect(retrievedBpmnXml).toContain('flowable:priority="75"');
    expect(retrievedBpmnXml).toContain('flowable:class="com.wfp.workflow.delegate.NoOpDelegate"');
    expect(retrievedBpmnXml).toContain('flowable:async="true"');
  });

  test('Re-deploy the retrieved XML', async ({ request }) => {
    const response = await request.post(`${GATEWAY_URL}/api/workflow/deployments`, {
      headers: authHeaders(token),
      data: {
        name: 'Flowable Properties Test Re-deploy',
        bpmnXml: retrievedBpmnXml,
      },
    });
    expect(response.status()).toBe(201);
    const body = await response.json();
    expect(body.deploymentId).toBeTruthy();

    // Get the new process definition ID (should be version 2)
    const listResponse = await request.get(`${GATEWAY_URL}/api/workflow/deployments`, {
      headers: authHeaders(token),
    });
    const definitions = await listResponse.json();
    const found = definitions.find(
      (pd: any) => pd.key === 'flowablePropsTest' && pd.deploymentId === body.deploymentId,
    );
    expect(found).toBeTruthy();
    expect(found.version).toBeGreaterThan(1);
    redeployedProcessDefinitionId = found.id;
  });

  test('Retrieve re-deployed XML and verify properties still intact', async ({ request }) => {
    const response = await request.get(
      `${GATEWAY_URL}/api/workflow/deployments/${redeployedProcessDefinitionId}/bpmn`,
      { headers: authHeaders(token) },
    );
    expect(response.status()).toBe(200);
    const body = await response.json();
    const xml = body.bpmnXml;

    expect(xml).toContain('flowable:initiator="initiator"');
    expect(xml).toContain('flowable:assignee');
    expect(xml).toContain('flowable:candidateGroups');
    expect(xml).toContain('flowable:formKey="taskForm"');
    expect(xml).toContain('flowable:priority="75"');
    expect(xml).toContain('flowable:class="com.wfp.workflow.delegate.NoOpDelegate"');
    expect(xml).toContain('flowable:async="true"');
  });
});

// ==========================================================================
// 12. Admin Portal UI Tests (using Playwright browser)
// ==========================================================================

test.describe.serial('12. Admin Portal UI Tests', () => {
  test('Login to admin portal as admin-a', async ({ page }) => {
    await page.goto(ADMIN_PORTAL_URL);

    // Should redirect to Keycloak login
    await expect(page).toHaveURL(/realms\/workflow-platform/, { timeout: 15000 });
    await expect(page.getByRole('heading', { name: 'Sign in to your account' })).toBeVisible({
      timeout: 10000,
    });

    // Fill in credentials
    await page.getByLabel('Username or email').fill('admin-a');
    await page.getByLabel('Password', { exact: true }).fill('password');
    await page.getByRole('button', { name: 'Sign In' }).click();

    // Should redirect back to admin portal dashboard
    await expect(page).toHaveURL(new RegExp(`${ADMIN_PORTAL_URL.replace('http://', '')}.*dashboard`), {
      timeout: 15000,
    });
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible({ timeout: 10000 });
  });

  test('Dashboard shows deployed processes count (not "--")', async ({ page }) => {
    await loginIfNeeded(page, ADMIN_PORTAL_URL, 'admin-a', 'password');

    // The "Deployed Processes" card should show a number, not "--" or 0 (we deployed processes earlier)
    const deployedCard = page.locator('text=Deployed Processes').locator('..');
    await expect(deployedCard).toBeVisible({ timeout: 10000 });

    // Wait for the number to load (not be the initial loading state)
    await page.waitForFunction(
      () => {
        const el = document.querySelector('h3');
        if (!el) return false;
        // Find the card containing "Deployed Processes"
        const cards = Array.from(document.querySelectorAll('h3'));
        const deployedH3 = cards.find((h) => h.textContent?.includes('Deployed Processes'));
        if (!deployedH3) return false;
        const parent = deployedH3.parentElement;
        const numberDiv = parent?.querySelector('div[style*="font-size"]');
        return numberDiv && numberDiv.textContent !== '--' && numberDiv.textContent !== '';
      },
      { timeout: 10000 },
    );
  });

  test('Navigate to Processes page, verify process list loads', async ({ page }) => {
    await loginIfNeeded(page, ADMIN_PORTAL_URL, 'admin-a', 'password');

    // Click on Processes nav link
    await page.getByRole('link', { name: 'Processes' }).click();
    await expect(page).toHaveURL(/processes/, { timeout: 10000 });

    // Verify the heading and table are present
    await expect(page.getByRole('heading', { name: 'Process Definitions' })).toBeVisible({
      timeout: 10000,
    });

    // Should see a table with at least one row (from our deployments)
    await expect(page.locator('table')).toBeVisible({ timeout: 10000 });
    // Wait for at least one row with data
    await expect(page.locator('table tbody tr').first()).toBeVisible({ timeout: 10000 });
  });

  test('Navigate to Audit Log, verify audit entries load', async ({ page }) => {
    await loginIfNeeded(page, ADMIN_PORTAL_URL, 'admin-a', 'password');

    // Click on Audit Log nav link
    await page.getByRole('link', { name: 'Audit Log' }).click();
    await expect(page).toHaveURL(/audit/, { timeout: 10000 });

    // Verify the heading
    await expect(page.getByRole('heading', { name: 'Audit Log' })).toBeVisible({ timeout: 10000 });

    // Should see a table with audit entries
    await expect(page.locator('table')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('table tbody tr').first()).toBeVisible({ timeout: 10000 });
  });
});

// ==========================================================================
// 13. User Portal UI Tests (using Playwright browser)
// ==========================================================================

test.describe.serial('13. User Portal UI Tests', () => {
  test('Login to user portal as user-a', async ({ page }) => {
    await page.goto(USER_PORTAL_URL);

    // Should redirect to Keycloak login
    await expect(page).toHaveURL(/realms\/workflow-platform/, { timeout: 15000 });
    await expect(page.getByRole('heading', { name: 'Sign in to your account' })).toBeVisible({
      timeout: 10000,
    });

    // Fill in credentials
    await page.getByLabel('Username or email').fill('user-a');
    await page.getByLabel('Password', { exact: true }).fill('password');
    await page.getByRole('button', { name: 'Sign In' }).click();

    // Should redirect back to user portal dashboard
    await expect(page).toHaveURL(new RegExp(`${USER_PORTAL_URL.replace('http://', '')}.*dashboard`), {
      timeout: 15000,
    });
    await expect(page.getByRole('heading', { name: /Welcome/ })).toBeVisible({ timeout: 10000 });
  });

  test('Dashboard shows task count and notification count', async ({ page }) => {
    await loginIfNeeded(page, USER_PORTAL_URL, 'user-a', 'password');

    // Verify "My Tasks" card exists
    await expect(page.locator('text=My Tasks').first()).toBeVisible({ timeout: 10000 });

    // Verify "Unread Notifications" card exists
    await expect(page.locator('text=Unread Notifications').first()).toBeVisible({ timeout: 10000 });

    // Verify "Start New Process" link card exists
    await expect(page.getByRole('link', { name: 'Start New Process' })).toBeVisible({ timeout: 10000 });
  });

  test('Navigate to My Tasks, verify task table loads', async ({ page }) => {
    await loginIfNeeded(page, USER_PORTAL_URL, 'user-a', 'password');

    // Click on My Tasks nav link
    await page.getByRole('link', { name: 'My Tasks' }).click();
    await expect(page).toHaveURL(/tasks/, { timeout: 10000 });

    // Verify heading
    await expect(page.getByRole('heading', { name: 'My Tasks' })).toBeVisible({ timeout: 10000 });

    // The table should be visible (even if empty, it renders the table)
    await expect(page.locator('table').first()).toBeVisible({ timeout: 10000 });
  });

  test('Navigate to Start Process, verify process cards appear', async ({ page }) => {
    await loginIfNeeded(page, USER_PORTAL_URL, 'user-a', 'password');

    // Click on Start Process nav link
    await page.getByRole('link', { name: 'Start Process' }).click();
    await expect(page).toHaveURL(/start-process/, { timeout: 10000 });

    // Verify heading
    await expect(page.getByRole('heading', { name: 'Start Process' })).toBeVisible({ timeout: 10000 });

    // Should see at least one process card with a "Start" button
    await expect(page.getByRole('button', { name: 'Start' }).first()).toBeVisible({ timeout: 10000 });
  });

  test('Click Start on a process, verify redirect to My Processes', async ({ page }) => {
    await loginIfNeeded(page, USER_PORTAL_URL, 'user-a', 'password');
    await page.goto(`${USER_PORTAL_URL}/start-process`);
    await page.waitForURL(/start-process/, { timeout: 15000 });

    // Wait for process cards to load, then click the first Start button
    const startButton = page.getByRole('button', { name: 'Start' }).first();
    await expect(startButton).toBeVisible({ timeout: 10000 });
    await startButton.click();

    // Should redirect to My Processes after starting
    await expect(page).toHaveURL(/my-processes/, { timeout: 15000 });
    await expect(page.getByRole('heading', { name: 'My Processes' })).toBeVisible({ timeout: 10000 });
  });

  test('Navigate to Notifications page', async ({ page }) => {
    await loginIfNeeded(page, USER_PORTAL_URL, 'user-a', 'password');

    // Click on Notifications nav link
    await page.getByRole('link', { name: 'Notifications' }).click();
    await expect(page).toHaveURL(/notifications/, { timeout: 10000 });

    // Verify heading
    await expect(page.getByRole('heading', { name: 'Notifications' })).toBeVisible({ timeout: 10000 });

    // Verify "Mark All Read" button exists
    await expect(page.getByRole('button', { name: 'Mark All Read' })).toBeVisible({ timeout: 10000 });
  });
});
