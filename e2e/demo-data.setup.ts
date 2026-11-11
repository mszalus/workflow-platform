import { expect, test as setup, type APIRequestContext } from '@playwright/test';
import fs from 'fs';
import path from 'path';

const API_URL = 'http://localhost:5175/api';
const TOKEN_URL = 'http://localhost:8180/realms/workflow-platform/protocol/openid-connect/token';
const PROJECT = 'DEMO';
const SAMPLE_WORKFLOW = fs.readFileSync(path.join(__dirname, 'samples', 'tracker-flow.bpmn20.xml'), 'utf-8');

const ITEMS = [
  { type: 'Bug', title: 'Login page loops after the session expires', priority: 'HIGH', assignee: 'user-a', severity: 'major', transitions: ['accept', 'startWork'] },
  { type: 'Task', title: 'Write the onboarding guide', priority: 'MEDIUM', assignee: 'user-a', transitions: ['accept'] },
  { type: 'Task', title: 'Upgrade the PostgreSQL driver', priority: 'LOW', assignee: 'admin-a', transitions: ['accept', 'startWork', 'finish'] },
  { type: 'Bug', title: 'Export drops rows with empty dates', priority: 'MEDIUM', assignee: 'user-a', severity: 'minor', transitions: [] },
  { type: 'Task', title: 'Review the access rules for contractors', priority: 'HIGH', assignee: 'admin-a', transitions: ['accept', 'startWork'] },
  { type: 'Bug', title: 'Attachment upload fails above 10 MB without a message', priority: 'LOW', severity: 'minor', transitions: ['accept'] },
  { type: 'Task', title: 'Plan the quarterly release', priority: 'MEDIUM', assignee: 'user-a', transitions: [] },
  { type: 'Task', title: 'Archive the old audit exports', priority: 'LOW', assignee: 'user-a', transitions: ['accept', 'startWork', 'finish', 'close'] },
];

async function client(request: APIRequestContext, username: string) {
  const login = await request.post(TOKEN_URL, {
    form: { grant_type: 'password', client_id: 'wfp-web', username, password: 'password' },
  });
  expect(login.status(), await login.text()).toBe(200);
  const headers = { Authorization: `Bearer ${(await login.json()).access_token}` };
  return {
    get: async (url: string) => (await request.get(`${API_URL}${url}`, { headers })).json(),
    post: async (url: string, data: object) => {
      const response = await request.post(`${API_URL}${url}`, { headers, data });
      expect(response.ok(), `POST ${url}: ${await response.text()}`).toBe(true);
      return response.json();
    },
  };
}

setup('demo project with items', async ({ request }) => {
  const admin = await client(request, 'admin-a');
  const user = await client(request, 'user-a');

  const workflows: { key: string }[] = await admin.get('/workflow/deployments');
  if (!workflows.some((workflow) => workflow.key === 'trackerFlow')) {
    await admin.post('/workflow/deployments', { name: 'trackerFlow', bpmnXml: SAMPLE_WORKFLOW });
  }

  const projects: { key: string }[] = await admin.get('/projects');
  if (projects.some((project) => project.key === PROJECT)) {
    return;
  }
  await admin.post('/projects', {
    key: PROJECT,
    name: 'Demo project',
    itemTypes: [
      { name: 'Task', workflowKey: 'default' },
      { name: 'Bug', workflowKey: 'default' },
    ],
  });
  await admin.post('/fields/schemas', {
    project: PROJECT,
    itemTypes: ['Bug'],
    fieldKey: 'severity',
    label: 'Severity',
    fieldType: 'DROPDOWN',
    options: [
      { label: 'Minor', value: 'minor', sortOrder: 0 },
      { label: 'Major', value: 'major', sortOrder: 1 },
    ],
  });

  for (const { type, title, priority, assignee, severity, transitions } of ITEMS) {
    const item = await user.post('/items', {
      project: PROJECT,
      type,
      title,
      priority,
      assignee,
      description: 'Sample item created by the demo data script.',
      fields: severity ? { severity } : {},
    });
    for (const transitionId of transitions) {
      await user.post(`/items/${item.key}/transitions`, { transitionId });
    }
  }
  await admin.post(`/items/${PROJECT}-1/comments`, {
    content: 'Reproduced on the staging stack. The refresh token is not renewed.',
  });
});
