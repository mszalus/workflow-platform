import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '../../theme';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App from '../../App';
import { ProjectProvider, type Project } from '../../projects/ProjectContext';

const api = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() }));
const editor = vi.hoisted(() => ({ onXmlChange: undefined as ((xml: string) => void) | undefined }));

vi.mock('../../shared-ui', () => ({
  useAuth: () => ({ username: 'admin-a', tenantId: 'tenant-a', roles: ['admin'], logout: vi.fn() }),
  apiClient: api,
}));
vi.mock('../../bpmn-editor', () => ({
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

const workflows = [
  { id: 'bug-flow:3:1', key: 'bug-flow', name: 'Bug workflow', version: 3, builtIn: false },
  { id: 'default:1:9', key: 'default', name: 'Default', version: 1, builtIn: true },
];

const defaultXml =
  '<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI">' +
  '<process id="default" name="Default"/><bpmndi:BPMNPlane bpmnElement="default"/></definitions>';

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
          <ThemeProvider>
            <App />
          </ThemeProvider>
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
    if (url === '/workflow/deployments/default:1:9/bpmn') return Promise.resolve({ data: { bpmnXml: defaultXml } });
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
    expect(form.getByRole('option', { name: 'default (built-in)' })).toBeTruthy();

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

describe('Editing a project', () => {
  async function openEditor() {
    renderAt('/admin/projects');
    fireEvent.click(screen.getByRole('button', { name: 'Edit PROJ' }));
    const form = within(screen.getByRole('form', { name: 'Edit project PROJ' }));
    await form.findByRole('option', { name: 'bug-flow' });
    return form;
  }

  it('fixes the key and the existing item types', async () => {
    const form = await openEditor();

    expect((form.getByLabelText('Key') as HTMLInputElement).disabled).toBe(true);
    expect((form.getAllByLabelText('Item type name')[0] as HTMLInputElement).disabled).toBe(true);
    expect((form.getAllByLabelText('Workflow')[0] as HTMLSelectElement).disabled).toBe(true);
  });

  it('saves a new name and a new item type', async () => {
    api.put.mockResolvedValue({ data: {} });
    const form = await openEditor();

    fireEvent.change(form.getByLabelText('Name'), { target: { value: 'Renamed' } });
    fireEvent.click(form.getByRole('button', { name: '+ Add item type' }));
    fireEvent.change(form.getAllByLabelText('Item type name')[2], { target: { value: 'Story' } });
    fireEvent.change(form.getAllByLabelText('Workflow')[2], { target: { value: 'bug-flow' } });
    fireEvent.click(form.getByRole('button', { name: 'Save' }));

    await waitFor(() =>
      expect(api.put).toHaveBeenCalledWith('/projects/PROJ', {
        name: 'Renamed',
        itemTypes: [
          { name: 'Bug', workflowKey: 'bug-flow' },
          { name: 'Task', workflowKey: 'simple' },
          { name: 'Story', workflowKey: 'bug-flow' },
        ],
      }),
    );
    await waitFor(() => expect(screen.queryByRole('form', { name: 'Edit project PROJ' })).toBeNull());
  });

  it('shows why an item type cannot be removed', async () => {
    api.put.mockRejectedValue({ response: { data: { message: 'Item type Bug still has items' } } });
    const form = await openEditor();

    fireEvent.click(form.getAllByRole('button', { name: 'Remove' })[0]);
    fireEvent.click(form.getByRole('button', { name: 'Save' }));

    expect(await form.findByText('Item type Bug still has items')).toBeTruthy();
    expect(screen.getByRole('form', { name: 'Edit project PROJ' })).toBeTruthy();
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

  it('marks the built-in workflow and gives it no Edit link', async () => {
    renderAt('/admin/workflows');

    const builtIn = (await screen.findByText('Built-in')).closest('tr')!;
    expect(within(builtIn).queryByRole('link', { name: 'Edit' })).toBeNull();
    expect(within(builtIn).getByRole('button', { name: 'Use as template' })).toBeTruthy();
  });

  it('offers a workflow of the tenant as a template too', async () => {
    renderAt('/admin/workflows');

    const own = (await screen.findByText('Bug workflow')).closest('tr')!;
    expect(within(own).getByRole('link', { name: 'Edit' })).toBeTruthy();
    expect(within(own).getByRole('button', { name: 'Use as template' })).toBeTruthy();
  });

  it('refuses the id of an existing workflow for the copy', async () => {
    renderAt('/admin/workflows');
    const builtIn = (await screen.findByText('Built-in')).closest('tr')!;
    fireEvent.click(within(builtIn).getByRole('button', { name: 'Use as template' }));

    const dialog = within(screen.getByRole('dialog', { name: 'Use as template' }));
    fireEvent.change(dialog.getByLabelText('New workflow id'), { target: { value: 'bug-flow' } });
    fireEvent.change(dialog.getByLabelText('New workflow name'), { target: { value: 'Copy' } });
    fireEvent.click(dialog.getByRole('button', { name: 'Open in editor' }));

    expect(dialog.getByRole('alert').textContent).toBe('A workflow with this id already exists.');
    expect(screen.queryByTestId('editor')).toBeNull();
  });

  it('copies the built-in workflow under a new id into the editor', async () => {
    renderAt('/admin/workflows');
    const builtIn = (await screen.findByText('Built-in')).closest('tr')!;
    fireEvent.click(within(builtIn).getByRole('button', { name: 'Use as template' }));

    const dialog = within(screen.getByRole('dialog', { name: 'Use as template' }));
    fireEvent.change(dialog.getByLabelText('New workflow id'), { target: { value: 'bugFlow2' } });
    fireEvent.change(dialog.getByLabelText('New workflow name'), { target: { value: 'Bug flow 2' } });
    fireEvent.click(dialog.getByRole('button', { name: 'Open in editor' }));

    const editor = await screen.findByTestId('editor');
    expect(editor.textContent).toContain('<process id="bugFlow2" name="Bug flow 2"/>');
    expect(editor.textContent).toContain('bpmnElement="bugFlow2"');
    expect((screen.getByLabelText('Workflow name') as HTMLInputElement).value).toBe('Bug flow 2');
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

describe('Deploying over open items', () => {
  const summary = { workflowKey: 'bug-flow', openItems: 4, sameStatus: 2, walkedBack: 1, reset: 1, failed: 0 };

  function mockPost(moved = summary) {
    api.post.mockImplementation((url: string) => {
      if (url === '/workflow/workflows/upgrade-preview') return Promise.resolve({ data: summary });
      if (url === '/workflow/workflows/bug-flow/upgrade') return Promise.resolve({ data: moved });
      return Promise.resolve({ data: [] });
    });
  }

  async function openDialog() {
    renderAt('/admin/workflows/bug-flow%3A3%3A1/edit');
    await screen.findByText('<saved/>');
    await waitFor(() =>
      expect((screen.getByRole('button', { name: 'Deploy' }) as HTMLButtonElement).disabled).toBe(false),
    );
    fireEvent.click(screen.getByRole('button', { name: 'Deploy' }));
    return within(await screen.findByRole('dialog', { name: 'Deploy bug-flow' }));
  }

  it('shows the dry run and deploys nothing until confirmed', async () => {
    mockPost();
    const dialog = await openDialog();

    expect(dialog.getByText('Dry run: same status 2 · walked back 1 · reset 1')).toBeTruthy();
    expect(api.post).not.toHaveBeenCalledWith('/workflow/deployments', expect.anything());
  });

  it('keeps items on their version by default', async () => {
    mockPost();
    const dialog = await openDialog();

    fireEvent.click(dialog.getByRole('button', { name: 'Deploy' }));

    await waitFor(() =>
      expect(api.post).toHaveBeenCalledWith('/workflow/deployments', { name: 'bug-flow', bpmnXml: '<saved/>' }),
    );
    expect(await screen.findByRole('heading', { name: 'Workflows' })).toBeTruthy();
    expect(api.post).not.toHaveBeenCalledWith('/workflow/workflows/bug-flow/upgrade');
  });

  it('moves items after deploying when asked to', async () => {
    mockPost();
    const dialog = await openDialog();

    fireEvent.click(dialog.getByLabelText(/Move them/));
    fireEvent.click(dialog.getByRole('button', { name: 'Deploy' }));

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/workflow/workflows/bug-flow/upgrade'));
    expect(await screen.findByRole('heading', { name: 'Workflows' })).toBeTruthy();
  });

  it('says so when some items could not be moved', async () => {
    mockPost({ ...summary, failed: 2 });
    const dialog = await openDialog();

    fireEvent.click(dialog.getByLabelText(/Move them/));
    fireEvent.click(dialog.getByRole('button', { name: 'Deploy' }));

    expect(await screen.findByText('Deploy failed: Deployed, but 2 items could not be moved.')).toBeTruthy();
  });

  it('cancels without deploying', async () => {
    mockPost();
    const dialog = await openDialog();

    fireEvent.click(dialog.getByRole('button', { name: 'Cancel' }));

    expect(screen.queryByRole('dialog')).toBeNull();
    expect(api.post).not.toHaveBeenCalledWith('/workflow/deployments', expect.anything());
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
