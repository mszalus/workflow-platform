import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App from '../../App';
import { ProjectProvider, type Project } from '../../projects/ProjectContext';

const api = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() }));
const editor = vi.hoisted(() => ({ onXmlChange: undefined as ((xml: string) => void) | undefined }));

vi.mock('@wfp/shared-ui', () => ({
  useAuth: () => ({ username: 'admin-a', tenantId: 'tenant-a', roles: ['admin'], logout: vi.fn() }),
  apiClient: api,
}));
vi.mock('@wfp/bpmn-editor', () => ({
  BpmnEditor: ({ xml, onXmlChange }: { xml?: string; onXmlChange: (xml: string) => void }) => {
    editor.onXmlChange = onXmlChange;
    return <pre data-testid="editor">{xml}</pre>;
  },
}));

const projects: Project[] = [
  {
    key: 'PROJ',
    name: 'Platform',
    itemTypes: [
      { name: 'Bug', workflowKey: 'bug-flow' },
      { name: 'Task', workflowKey: 'simple' },
    ],
  },
];

const workflows = [{ id: 'bug-flow:3:1', key: 'bug-flow', name: 'Bug workflow', version: 3 }];

const severity = {
  id: 'f1',
  fieldKey: 'severity',
  label: 'Severity',
  fieldType: 'DROPDOWN',
  required: true,
  itemTypes: ['Bug'],
};

const auditEntry = {
  id: 'a1',
  eventType: 'item.created',
  entityId: 'PROJ-1',
  userId: 'user-a',
  timestamp: '2026-11-01T10:00:00Z',
  details: { title: 'Fix login' },
};

function renderAt(path: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  queryClient.setQueryData(['projects'], projects);
  return render(
    <QueryClientProvider client={queryClient}>
      <ProjectProvider>
        <MemoryRouter initialEntries={[path]}>
          <App />
        </MemoryRouter>
      </ProjectProvider>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  localStorage.clear();
  api.post.mockReset().mockResolvedValue({ data: {} });
  api.delete.mockReset().mockResolvedValue({ data: undefined });
  api.get.mockReset().mockImplementation((url: string) => {
    if (url === '/workflow/deployments') return Promise.resolve({ data: workflows });
    if (url === '/workflow/deployments/bug-flow:3:1/bpmn') return Promise.resolve({ data: { bpmnXml: '<saved/>' } });
    if (url === '/fields/schemas') return Promise.resolve({ data: [severity] });
    if (url === '/audit') return Promise.resolve({ data: { content: [auditEntry], totalPages: 1, last: true } });
    if (url === '/notifications/unread-count') return Promise.resolve({ data: { count: 0 } });
    return Promise.resolve({ data: projects });
  });
});

describe('Projects', () => {
  it('lists projects with their item types', () => {
    renderAt('/admin/projects');

    expect(screen.getByText('Bug -> bug-flow, Task -> simple')).toBeTruthy();
  });

  it('creates a project with an item type on a deployed workflow', async () => {
    renderAt('/admin/projects');
    fireEvent.click(screen.getByRole('button', { name: '+ New project' }));
    const form = within(screen.getByRole('form', { name: 'New project' }));
    await form.findByRole('option', { name: 'bug-flow' });

    fireEvent.change(form.getByLabelText('Key'), { target: { value: 'OPS' } });
    fireEvent.change(form.getByLabelText('Name'), { target: { value: 'Operations' } });
    fireEvent.change(form.getByLabelText('Item type name'), { target: { value: 'Incident' } });
    fireEvent.change(form.getByLabelText('Workflow'), { target: { value: 'bug-flow' } });
    fireEvent.click(form.getByRole('button', { name: 'Save' }));

    await waitFor(() =>
      expect(api.post).toHaveBeenCalledWith('/projects', {
        key: 'OPS',
        name: 'Operations',
        itemTypes: [{ name: 'Incident', workflowKey: 'bug-flow' }],
      }),
    );
    await waitFor(() => expect(screen.queryByRole('form', { name: 'New project' })).toBeNull());
  });

  it('shows why the server refused a project', async () => {
    api.post.mockRejectedValue({ response: { data: { message: 'Project OPS already exists' } } });
    renderAt('/admin/projects');
    fireEvent.click(screen.getByRole('button', { name: '+ New project' }));
    const form = within(screen.getByRole('form', { name: 'New project' }));
    await form.findByRole('option', { name: 'bug-flow' });

    fireEvent.change(form.getByLabelText('Key'), { target: { value: 'OPS' } });
    fireEvent.change(form.getByLabelText('Name'), { target: { value: 'Operations' } });
    fireEvent.change(form.getByLabelText('Item type name'), { target: { value: 'Incident' } });
    fireEvent.change(form.getByLabelText('Workflow'), { target: { value: 'bug-flow' } });
    fireEvent.click(form.getByRole('button', { name: 'Save' }));

    expect(await form.findByText('Project OPS already exists')).toBeTruthy();
  });
});

describe('Workflows', () => {
  it('lists the latest version and links to the editor', async () => {
    renderAt('/admin/workflows');

    expect(await screen.findByText('v3')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Edit' }).getAttribute('href')).toBe(
      '/admin/workflows/bug-flow%3A3%3A1/edit',
    );
  });
});

describe('Workflow editor', () => {
  it('opens a saved workflow and deploys it under its key', async () => {
    renderAt('/admin/workflows/bug-flow%3A3%3A1/edit');

    expect(await screen.findByText('<saved/>')).toBeTruthy();
    await waitFor(() => expect((screen.getByLabelText('Workflow name') as HTMLInputElement).value).toBe('bug-flow'));
    fireEvent.click(screen.getByRole('button', { name: 'Deploy' }));

    await waitFor(() =>
      expect(api.post).toHaveBeenCalledWith('/workflow/deployments', { name: 'bug-flow', bpmnXml: '<saved/>' }),
    );
    expect(await screen.findByRole('heading', { name: 'Workflows' })).toBeTruthy();
  });

  it('shows the violations when a deploy is refused', async () => {
    api.post.mockImplementation((url: string) =>
      url === '/workflow/deployments'
        ? Promise.reject({
            response: {
              data: {
                message: 'Invalid workflow',
                details: { violations: [{ elementId: 'open', rule: 5, message: 'Open has no way out' }] },
              },
            },
          })
        : Promise.resolve({ data: [] }),
    );
    renderAt('/admin/workflows/bug-flow%3A3%3A1/edit');
    await screen.findByText('<saved/>');
    await waitFor(() =>
      expect((screen.getByRole('button', { name: 'Deploy' }) as HTMLButtonElement).disabled).toBe(false),
    );

    fireEvent.click(screen.getByRole('button', { name: 'Deploy' }));

    expect(await screen.findByText('Deploy failed: Invalid workflow')).toBeTruthy();
    expect(screen.getByText(/Open has no way out/)).toBeTruthy();
  });
});

describe('Fields', () => {
  it('lists the fields of the current project', async () => {
    renderAt('/admin/fields');

    expect(await screen.findByText('Severity')).toBeTruthy();
    expect(screen.getByText('Select')).toBeTruthy();
    expect(screen.getByText('Bug')).toBeTruthy();
  });

  it('creates a select field limited to an item type', async () => {
    renderAt('/admin/fields');
    fireEvent.click(await screen.findByRole('button', { name: '+ New field' }));
    const form = within(screen.getByRole('form', { name: 'New field' }));

    fireEvent.change(form.getByLabelText('Name'), { target: { value: 'Component' } });
    fireEvent.change(form.getByLabelText('Key'), { target: { value: 'component' } });
    fireEvent.change(form.getByLabelText('Type'), { target: { value: 'DROPDOWN' } });
    fireEvent.click(form.getByLabelText('Task'));
    fireEvent.change(form.getByLabelText('Options, one per line'), { target: { value: 'Auth\n\nBilling, EU\n' } });
    fireEvent.click(form.getByRole('button', { name: 'Save' }));

    await waitFor(() =>
      expect(api.post).toHaveBeenCalledWith('/fields/schemas', {
        project: 'PROJ',
        itemTypes: ['Task'],
        fieldKey: 'component',
        label: 'Component',
        fieldType: 'DROPDOWN',
        required: false,
        options: [
          { label: 'Auth', value: 'Auth', sortOrder: 0 },
          { label: 'Billing, EU', value: 'Billing, EU', sortOrder: 1 },
        ],
      }),
    );
  });

  it('deletes a field only after confirmation', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValueOnce(true);
    renderAt('/admin/fields');
    const deleteButton = await screen.findByRole('button', { name: 'Delete Severity' });

    fireEvent.click(deleteButton);
    expect(api.delete).not.toHaveBeenCalled();

    fireEvent.click(deleteButton);
    await waitFor(() => expect(api.delete).toHaveBeenCalledWith('/fields/schemas/f1'));
    confirm.mockRestore();
  });
});

describe('Audit log', () => {
  const lastRequest = () => api.get.mock.calls.filter(([url]) => url === '/audit').at(-1)![1].params as URLSearchParams;

  it('lists entries', async () => {
    renderAt('/admin/audit');

    expect(await screen.findByText('item.created')).toBeTruthy();
    expect(screen.getByText('{"title":"Fix login"}')).toBeTruthy();
  });

  it('applies the filters', async () => {
    renderAt('/admin/audit');
    await screen.findByText('item.created');

    fireEvent.change(screen.getByLabelText('Who'), { target: { value: 'user-a' } });
    fireEvent.change(screen.getByLabelText('What'), { target: { value: 'item.created' } });
    fireEvent.change(screen.getByLabelText('From'), { target: { value: '2026-11-01' } });
    fireEvent.change(screen.getByLabelText('To'), { target: { value: '2026-11-02' } });
    fireEvent.click(screen.getByRole('button', { name: 'Apply' }));

    await waitFor(() => expect(lastRequest().get('userId')).toBe('user-a'));
    expect(lastRequest().get('eventType')).toBe('item.created');
    expect(lastRequest().get('from')).toBe('2026-11-01T00:00:00Z');
    expect(lastRequest().get('to')).toBe('2026-11-02T23:59:59Z');
  });
});
