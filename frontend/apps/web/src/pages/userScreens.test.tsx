import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App from '../App';
import { ProjectProvider, type Project } from '../projects/ProjectContext';

const api = vi.hoisted(() => ({ get: vi.fn(), put: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() }));

vi.mock('../shared-ui', () => ({
  useAuth: () => ({ username: 'user-a', tenantId: 'tenant-a', roles: ['user'], logout: vi.fn() }),
  apiClient: api,
}));
vi.mock('../components/ItemDiagram', () => ({ default: () => null }));

const projects: Project[] = [
  {
    key: 'PROJ',
    name: 'Platform',
    itemTypes: [
      { name: 'Bug', workflowKey: 'bug-flow' },
      { name: 'Task', workflowKey: 'task-flow' },
    ],
  },
];

const answerField = {
  id: 'f1',
  fieldKey: 'answer',
  label: 'Answer',
  fieldType: 'DROPDOWN',
  required: true,
  sortOrder: 0,
  options: [
    { id: 'o1', label: 'Yes, please', value: 'Yes, please', sortOrder: 0 },
    { id: 'o2', label: 'No', value: 'No', sortOrder: 1 },
  ],
};

function item(overrides = {}) {
  return {
    key: 'PROJ-1',
    project: 'PROJ',
    type: 'Bug',
    title: 'Fix login',
    description: 'It redirects badly',
    priority: 'MEDIUM',
    assignee: 'user-a',
    reporter: 'user-b',
    statusId: 'open',
    statusName: 'Open',
    statusCategory: 'OPEN',
    fields: { answer: 'No' },
    workflowVersionId: 'bug-flow:1:1',
    createdAt: '2026-11-01T10:00:00Z',
    updatedAt: new Date().toISOString(),
    transitions: [{ id: 'startWork', name: 'Start work' }],
    openSteps: [],
    ...overrides,
  };
}

function page(content: unknown[]) {
  return { content, page: 0, size: 25, totalElements: content.length, totalPages: 1, last: true };
}

const history = [
  {
    timestamp: new Date().toISOString(),
    actor: 'user-a',
    kind: 'STATUS',
    from: 'Open',
    to: 'Doing',
    transition: 'Start work',
  },
  {
    timestamp: new Date().toISOString(),
    actor: 'user-b',
    kind: 'FIELD',
    field: 'fields.answer',
    from: 'No',
    to: 'Yes',
  },
  { timestamp: new Date().toISOString(), actor: 'user-b', kind: 'FIELD', field: 'description', from: 'a', to: 'b' },
  { timestamp: new Date().toISOString(), actor: 'user-b', kind: 'CREATED' },
];

const attachments = [
  { id: 'a1', fileName: 'mine.pdf', sizeBytes: 2048, uploadedBy: 'user-a', uploadedAt: new Date().toISOString() },
  { id: 'a2', fileName: 'theirs.png', sizeBytes: 512, uploadedBy: 'user-b', uploadedAt: new Date().toISOString() },
];

let itemsResponse = page([]);

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

function itemRequests() {
  return api.get.mock.calls.filter(([url]) => url === '/items').map(([, config]) => config.params as URLSearchParams);
}

beforeEach(() => {
  localStorage.clear();
  itemsResponse = page([]);
  api.put.mockReset().mockResolvedValue({ data: undefined });
  api.post.mockReset();
  api.patch.mockReset();
  api.delete.mockReset();
  api.get.mockReset().mockImplementation((url: string, config?: { params: URLSearchParams }) => {
    if (url === '/items') {
      const reportedByMe = config?.params.get('filter') === 'reporter:eq:me';
      return Promise.resolve({ data: reportedByMe ? page([]) : itemsResponse });
    }
    if (url === '/items/PROJ-1') return Promise.resolve({ data: item() });
    if (url === '/items/PROJ-1/comments') {
      return Promise.resolve({
        data: [{ id: 'c1', userId: 'user-b', content: 'Reproduced', createdAt: '2026-11-01T10:00:00Z' }],
      });
    }
    if (url === '/items/PROJ-1/history') return Promise.resolve({ data: history });
    if (url === '/items/PROJ-1/attachments') return Promise.resolve({ data: attachments });
    if (url === '/fields/schemas') return Promise.resolve({ data: [answerField] });
    if (url === '/notifications/unread-count') return Promise.resolve({ data: { count: 0 } });
    return Promise.resolve({ data: projects });
  });
});

describe('My work', () => {
  it('groups my items by status category and leaves out old done items', async () => {
    itemsResponse = page([
      item({ key: 'PROJ-1', title: 'In flight', statusCategory: 'IN_PROGRESS' }),
      item({ key: 'PROJ-2', title: 'Just finished', statusCategory: 'DONE' }),
      item({ key: 'PROJ-3', title: 'Long finished', statusCategory: 'DONE', updatedAt: '2020-01-01T00:00:00Z' }),
    ]);

    renderAt('/my-work');

    expect(await screen.findByRole('heading', { name: 'IN PROGRESS (1)' })).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'DONE, last 7 days (1)' })).toBeTruthy();
    expect(screen.queryByText('Long finished')).toBeNull();
    expect(itemRequests()[0].getAll('filter')).toEqual(['assignee:eq:me']);
  });
});

describe('Items', () => {
  const lastRequest = () => itemRequests().at(-1)!;

  async function addFilterOn(fieldKey: string) {
    fireEvent.click(await screen.findByRole('button', { name: '+ Add filter' }));
    await screen.findByRole('option', { name: 'Answer (cf)' });
    fireEvent.change(screen.getByLabelText('Field'), { target: { value: fieldKey } });
  }

  it('lists the current project', async () => {
    itemsResponse = page([item()]);

    renderAt('/items');

    expect(await screen.findByRole('link', { name: 'PROJ-1' })).toBeTruthy();
    expect(lastRequest().get('project')).toBe('PROJ');
    expect(lastRequest().get('sort')).toBe('updatedAt,desc');
  });

  it('escapes a comma inside a selected value', async () => {
    renderAt('/items');
    await addFilterOn('cf.answer');
    fireEvent.change(screen.getByLabelText('Operator'), { target: { value: 'in' } });

    const values = screen.getByLabelText('Value') as HTMLSelectElement;
    Array.from(values.options).forEach((option) => (option.selected = true));
    fireEvent.change(values);
    fireEvent.click(screen.getByRole('button', { name: 'Apply' }));

    await waitFor(() => expect(lastRequest().getAll('filter')).toEqual(['cf.answer:in:Yes\\, please,No']));
  });

  it('skips a filter row with no value', async () => {
    renderAt('/items');
    await addFilterOn('assignee');

    fireEvent.click(screen.getByRole('button', { name: 'Apply' }));

    await waitFor(() => expect(itemRequests().length).toBeGreaterThan(0));
    expect(lastRequest().getAll('filter')).toEqual([]);
  });

  it('sorts when a column header is clicked', async () => {
    renderAt('/items');

    fireEvent.click(await screen.findByRole('button', { name: 'Title' }));

    await waitFor(() => expect(lastRequest().get('sort')).toBe('title,asc'));
  });
});

describe('Create item', () => {
  async function openDialog() {
    fireEvent.click(await screen.findByRole('button', { name: '+ Create' }));
    return screen.findByRole('dialog', { name: 'Create item' });
  }

  it('creates an item with its custom fields and opens it', async () => {
    api.post.mockResolvedValue({ data: item() });
    renderAt('/my-work');
    const dialog = within(await openDialog());

    fireEvent.change(dialog.getByLabelText('Title'), { target: { value: 'New bug' } });
    fireEvent.change(await dialog.findByLabelText('Answer *'), { target: { value: 'No' } });
    fireEvent.click(dialog.getByRole('button', { name: 'Create' }));

    await waitFor(() =>
      expect(api.post).toHaveBeenCalledWith('/items', {
        project: 'PROJ',
        type: 'Bug',
        title: 'New bug',
        description: '',
        assignee: undefined,
        priority: 'MEDIUM',
        fields: { answer: 'No' },
      }),
    );
    expect(await screen.findByRole('heading', { name: 'Fix login' })).toBeTruthy();
  });

  it('shows why the server refused an item', async () => {
    api.post.mockRejectedValue({ response: { data: { message: "Field 'answer' is required" } } });
    renderAt('/my-work');
    const dialog = within(await openDialog());

    fireEvent.change(dialog.getByLabelText('Title'), { target: { value: 'New bug' } });
    fireEvent.change(await dialog.findByLabelText('Answer *'), { target: { value: 'No' } });
    fireEvent.click(dialog.getByRole('button', { name: 'Create' }));

    expect(await dialog.findByText("Field 'answer' is required")).toBeTruthy();
  });

  it('asks for the definitions of the chosen item type', async () => {
    renderAt('/my-work');
    const dialog = within(await openDialog());

    fireEvent.change(dialog.getByLabelText('Type'), { target: { value: 'Task' } });

    await waitFor(() => {
      const requests = api.get.mock.calls.filter(([url]) => url === '/fields/schemas');
      expect(requests.at(-1)![1].params.get('type')).toBe('Task');
    });
  });
});

describe('Item detail', () => {
  it('shows the item with its comments', async () => {
    renderAt('/items/PROJ-1');

    expect(await screen.findByRole('heading', { name: 'Fix login' })).toBeTruthy();
    expect(screen.getByText('It redirects badly')).toBeTruthy();
    expect(await screen.findByText('Reproduced')).toBeTruthy();
  });

  it('shows the history newest first', async () => {
    renderAt('/items/PROJ-1');

    expect(await screen.findByText('Open -> Doing (Start work)')).toBeTruthy();
    expect(screen.getByText('answer No -> Yes')).toBeTruthy();
    expect(screen.getByText('description changed')).toBeTruthy();
    expect(screen.getByText('created')).toBeTruthy();
  });

  it('moves the item and shows its new status', async () => {
    api.post.mockResolvedValue({ data: item({ statusName: 'Doing', transitions: [] }) });
    renderAt('/items/PROJ-1');

    fireEvent.click(await screen.findByRole('button', { name: 'Start work' }));

    expect(await screen.findByText('Doing')).toBeTruthy();
    expect(api.post).toHaveBeenCalledWith('/items/PROJ-1/transitions', { transitionId: 'startWork' });
  });

  it('completes an open step', async () => {
    api.get.mockImplementationOnce(() =>
      Promise.resolve({ data: item({ transitions: [], openSteps: [{ id: 'review', name: 'Review' }] }) }),
    );
    api.get.mockImplementation((url: string) => {
      if (url === '/items/PROJ-1') {
        return Promise.resolve({ data: item({ transitions: [], openSteps: [{ id: 'review', name: 'Review' }] }) });
      }
      const emptyLists = ['/items/PROJ-1/comments', '/items/PROJ-1/attachments', '/fields/schemas'];
      return Promise.resolve({ data: emptyLists.includes(url) ? [] : projects });
    });
    api.post.mockResolvedValue({ data: item({ transitions: [], openSteps: [] }) });
    renderAt('/items/PROJ-1');

    fireEvent.click(await screen.findByRole('button', { name: 'Complete: Review' }));

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/items/PROJ-1/steps', { stepId: 'review' }));
  });

  it('lists attachments and offers delete only on my own', async () => {
    renderAt('/items/PROJ-1');

    expect(await screen.findByRole('button', { name: 'theirs.png' })).toBeTruthy();
    expect(screen.getByText(/2 KB · user-a/)).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Delete mine.pdf' })).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Delete theirs.png' })).toBeNull();
  });

  it('uploads a chosen file', async () => {
    api.post.mockResolvedValue({ data: {} });
    renderAt('/items/PROJ-1');

    const file = new File(['hello'], 'notes.txt');
    fireEvent.change(await screen.findByLabelText(/Attach a file/), { target: { files: [file] } });

    await waitFor(() => expect(api.post).toHaveBeenCalled());
    const [url, form, config] = api.post.mock.calls[0];
    expect(url).toBe('/items/PROJ-1/attachments');
    expect((form as FormData).get('file')).toBe(file);
    expect(config.headers['Content-Type']).toBe('multipart/form-data');
  });

  it('refuses a file over the limit without uploading it', async () => {
    renderAt('/items/PROJ-1');

    const file = new File([new Uint8Array(10 * 1024 * 1024 + 1)], 'big.bin');
    fireEvent.change(await screen.findByLabelText(/Attach a file/), { target: { files: [file] } });

    expect((await screen.findByRole('alert')).textContent).toBe('Files can be at most 10 MB');
    expect(api.post).not.toHaveBeenCalled();
  });

  it('deletes an attachment after confirmation', async () => {
    api.delete.mockResolvedValue({ data: undefined });
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);
    renderAt('/items/PROJ-1');

    fireEvent.click(await screen.findByRole('button', { name: 'Delete mine.pdf' }));

    await waitFor(() => expect(api.delete).toHaveBeenCalledWith('/items/PROJ-1/attachments/a1'));
    confirm.mockRestore();
  });

  it('posts a comment', async () => {
    api.post.mockResolvedValue({ data: {} });
    renderAt('/items/PROJ-1');

    fireEvent.change(await screen.findByLabelText('Add a comment'), { target: { value: 'On it' } });
    fireEvent.click(screen.getByRole('button', { name: 'Post' }));

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/items/PROJ-1/comments', { content: 'On it' }));
  });

  it('saves the assignee, priority and custom fields', async () => {
    api.patch.mockResolvedValue({ data: item({ priority: 'HIGH' }) });
    renderAt('/items/PROJ-1');

    fireEvent.change(await screen.findByLabelText('Priority'), { target: { value: 'HIGH' } });
    fireEvent.change(await screen.findByLabelText('Answer *'), { target: { value: 'Yes, please' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() =>
      expect(api.patch).toHaveBeenCalledWith('/items/PROJ-1', {
        assignee: 'user-a',
        priority: 'HIGH',
        fields: { answer: 'Yes, please' },
      }),
    );
  });

  it('edits the description', async () => {
    api.patch.mockResolvedValue({ data: item({ description: 'Clearer' }) });
    renderAt('/items/PROJ-1');

    fireEvent.click(await screen.findByRole('button', { name: 'Edit' }));
    fireEvent.change(screen.getByLabelText('Description'), { target: { value: 'Clearer' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save description' }));

    await waitFor(() => expect(api.patch).toHaveBeenCalledWith('/items/PROJ-1', { description: 'Clearer' }));
    expect(await screen.findByText('Clearer')).toBeTruthy();
  });

  it('explains an item that cannot be loaded', async () => {
    api.get.mockImplementation((url: string) =>
      url === '/items/PROJ-1'
        ? Promise.reject({ response: { data: { message: 'Item not found: PROJ-1' } } })
        : Promise.resolve({ data: [] }),
    );
    renderAt('/items/PROJ-1');

    expect(await screen.findByText('Item not found: PROJ-1')).toBeTruthy();
  });
});
