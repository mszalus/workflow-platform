import { test, expect, type APIRequestContext } from '@playwright/test';

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const KEYCLOAK_URL = 'http://localhost:8180';
const API_URL = 'http://localhost:8081';
const TOKEN_ENDPOINT = `${KEYCLOAK_URL}/realms/workflow-platform/protocol/openid-connect/token`;
const OIDC_DISCOVERY = `${KEYCLOAK_URL}/realms/workflow-platform/.well-known/openid-configuration`;

const USERS = {
  'admin-a': { username: 'admin-a', password: 'password', tenant: 'tenant-a' },
  'user-a': { username: 'user-a', password: 'password', tenant: 'tenant-a' },
  'admin-b': { username: 'admin-b', password: 'password', tenant: 'tenant-b' },
  'user-b': { username: 'user-b', password: 'password', tenant: 'tenant-b' },
} as const;

// ---------------------------------------------------------------------------
// BPMN XML templates
// ---------------------------------------------------------------------------

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
  clientId = 'wfp-web',
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
  expect(response.status(), `Keycloak token for ${username}: ${await response.text()}`).toBe(200);

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

  test('Backend service healthy (port 8081)', async ({ request }) => {
    const services = [
      { port: 8081, name: 'app' },
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

  test('Token works against the API', async ({ request }) => {
    const token = await getToken(request, 'admin-a');
    const response = await request.get(`${API_URL}/api/workflow/deployments`, {
      headers: authHeaders(token),
    });
    expect(response.status()).toBe(200);
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
    const response = await request.get(`${API_URL}/api/notifications/unread-count`, {
      headers: authHeaders(token),
    });
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(typeof body.count).toBe('number');
    expect(body.count).toBeGreaterThanOrEqual(0);
  });

  test('List notifications', async ({ request }) => {
    const response = await request.get(`${API_URL}/api/notifications`, {
      headers: authHeaders(token),
    });
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.content).toBeTruthy();
    expect(Array.isArray(body.content)).toBe(true);
  });

  test('Mark all read', async ({ request }) => {
    const response = await request.put(`${API_URL}/api/notifications/mark-all-read`, {
      headers: authHeaders(token),
    });
    expect(response.status()).toBe(200);
  });

  test('Verify unread count is 0 after mark-all-read', async ({ request }) => {
    const response = await request.get(`${API_URL}/api/notifications/unread-count`, {
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
  test('Query audit events by eventType', async ({ request }) => {
    const token = await getToken(request, 'admin-a');

    const response = await request.get(`${API_URL}/api/audit?eventType=item.created`, {
      headers: authHeaders(token),
    });
    expect(response.status()).toBe(200);
    const body = await response.json();
    // Every returned event should match the filter
    for (const entry of body.content) {
      expect(entry.eventType).toBe('item.created');
    }
  });

  test('Query audit events by userId', async ({ request }) => {
    const token = await getToken(request, 'admin-a');

    const response = await request.get(`${API_URL}/api/audit?userId=admin-a`, {
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
    const response = await request.post(`${API_URL}/api/workflow/deployments`, {
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
    const response = await request.get(`${API_URL}/api/workflow/deployments`, {
      headers: authHeaders(tokenAdminB),
    });
    expect(response.status()).toBe(200);
    const definitions = await response.json();

    // tenant-b should NOT see the tenantIsolationTest process from tenant-a
    const found = definitions.find((pd: any) => pd.key === 'tenantIsolationTest');
    expect(found, 'Tenant-b should NOT see tenant-a process definition').toBeFalsy();
  });

});

// ==========================================================================
// 10. Negative Tests
// ==========================================================================

test.describe('10. Negative Tests', () => {
  test('Access API without token — expect 401', async ({ request }) => {
    const response = await request.get(`${API_URL}/api/workflow/deployments`);
    expect(response.status()).toBe(401);
  });

  test('Access with invalid/expired token — expect 401', async ({ request }) => {
    const response = await request.get(`${API_URL}/api/workflow/deployments`, {
      headers: { Authorization: 'Bearer invalid.jwt.token_that_is_clearly_not_valid' },
    });
    expect(response.status()).toBe(401);
  });

  test('Access with malformed Authorization header — expect 401', async ({ request }) => {
    const response = await request.get(`${API_URL}/api/workflow/deployments`, {
      headers: { Authorization: 'NotBearer sometoken' },
    });
    expect(response.status()).toBe(401);
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
    const response = await request.post(`${API_URL}/api/workflow/deployments`, {
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
    const listResponse = await request.get(`${API_URL}/api/workflow/deployments`, {
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
      `${API_URL}/api/workflow/deployments/${processDefinitionId}/bpmn`,
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
    const response = await request.post(`${API_URL}/api/workflow/deployments`, {
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
    const listResponse = await request.get(`${API_URL}/api/workflow/deployments`, {
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
      `${API_URL}/api/workflow/deployments/${redeployedProcessDefinitionId}/bpmn`,
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
