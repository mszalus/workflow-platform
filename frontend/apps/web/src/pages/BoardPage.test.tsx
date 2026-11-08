import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createEvent, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ProjectProvider, type Project } from '../projects/ProjectContext';
import BoardPage from './BoardPage';

const api = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn() }));

vi.mock('@wfp/shared-ui', () => ({ apiClient: api }));

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

const statuses = [
  { id: 'open', name: 'Open', category: 'OPEN' },
  { id: 'doing', name: 'Doing', category: 'IN_PROGRESS' },
  { id: 'triage', name: 'In Triage', category: 'IN_PROGRESS' },
  { id: 'done', name: 'Done', category: 'DONE' },
];

function item(overrides = {}) {
  return {
    key: 'PROJ-1',
    type: 'Bug',
    title: 'Fix login',
    assignee: 'user-a',
    statusName: 'Open',
    statusCategory: 'OPEN',
    ...overrides,
  };
}

let boardItems = [item()];
let detail: Record<string, unknown>;

function renderBoard() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  queryClient.setQueryData(['projects'], projects);
  return render(
    <QueryClientProvider client={queryClient}>
      <ProjectProvider>
        <MemoryRouter>
          <BoardPage />
        </MemoryRouter>
      </ProjectProvider>
    </QueryClientProvider>,
  );
}

async function drop(key: string, columnName: string) {
  const card = await screen.findByTestId(`card-${key}`);
  const dataTransfer = {
    data: '',
    setData: (_: string, v: string) => (dataTransfer.data = v),
    getData: () => dataTransfer.data,
  };
  fireEvent.dragStart(card, { dataTransfer });
  const column = screen.getByRole('region', { name: columnName });
  const event = createEvent.drop(column);
  Object.defineProperty(event, 'dataTransfer', { value: dataTransfer });
  fireEvent(column, event);
}

beforeEach(() => {
  localStorage.clear();
  boardItems = [item()];
  detail = {
    ...item(),
    statuses,
    transitions: [
      { id: 'startWork', name: 'Start work', targetStatusIds: ['doing'] },
      { id: 'finish', name: 'Finish', targetStatusIds: ['END'] },
    ],
  };
  api.post.mockReset().mockResolvedValue({ data: {} });
  api.get.mockReset().mockImplementation((url: string) => {
    if (url === '/items') {
      return Promise.resolve({ data: { content: boardItems, last: true } });
    }
    if (url === '/items/PROJ-1') return Promise.resolve({ data: detail });
    return Promise.resolve({ data: projects });
  });
});

describe('Board', () => {
  it('puts each item in the column of its status category and shows its status', async () => {
    boardItems = [item(), item({ key: 'PROJ-2', statusName: 'Doing', statusCategory: 'IN_PROGRESS' })];

    renderBoard();

    const inProgress = within(screen.getByRole('region', { name: 'In progress' }));
    expect(await inProgress.findByText('PROJ-2')).toBeTruthy();
    expect(inProgress.getByText('Doing')).toBeTruthy();
    expect(within(screen.getByRole('region', { name: 'Open' })).getByText('PROJ-1')).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'IN PROGRESS (1)' })).toBeTruthy();
  });

  it('runs the only transition that leads to the column', async () => {
    renderBoard();

    await drop('PROJ-1', 'In progress');

    await waitFor(() =>
      expect(api.post).toHaveBeenCalledWith('/items/PROJ-1/transitions', { transitionId: 'startWork' }),
    );
  });

  it('counts a transition to the end as moving to Done', async () => {
    renderBoard();

    await drop('PROJ-1', 'Done');

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/items/PROJ-1/transitions', { transitionId: 'finish' }));
  });

  it('asks which transition to run when several lead to the column', async () => {
    detail.transitions = [
      { id: 'startWork', name: 'Start work', targetStatusIds: ['doing'] },
      { id: 'triage', name: 'Triage', targetStatusIds: ['triage'] },
    ];
    renderBoard();

    await drop('PROJ-1', 'In progress');
    const picker = within(await screen.findByRole('dialog', { name: 'Choose a transition' }));
    expect(api.post).not.toHaveBeenCalled();

    fireEvent.click(picker.getByLabelText('Triage -> In Triage'));
    fireEvent.click(picker.getByRole('button', { name: 'Move' }));

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/items/PROJ-1/transitions', { transitionId: 'triage' }));
  });

  it('runs nothing when the picker is cancelled', async () => {
    detail.transitions = [
      { id: 'startWork', name: 'Start work', targetStatusIds: ['doing'] },
      { id: 'triage', name: 'Triage', targetStatusIds: ['triage'] },
    ];
    renderBoard();

    await drop('PROJ-1', 'In progress');
    fireEvent.click(await screen.findByRole('button', { name: 'Cancel' }));

    expect(screen.queryByRole('dialog')).toBeNull();
    expect(api.post).not.toHaveBeenCalled();
  });

  it('refuses a drop no transition can satisfy', async () => {
    renderBoard();

    await drop('PROJ-1', 'To do');

    expect(await screen.findByRole('alert')).toHaveProperty('textContent', 'PROJ-1 cannot move from Open to To do.');
    expect(api.post).not.toHaveBeenCalled();
  });

  it('does nothing when an item is dropped on its own column', async () => {
    renderBoard();

    await drop('PROJ-1', 'Open');

    expect(api.get).not.toHaveBeenCalledWith('/items/PROJ-1');
    expect(api.post).not.toHaveBeenCalled();
  });

  it('shows why the server refused a move', async () => {
    api.post.mockRejectedValue({ response: { data: { message: 'PROJ-1 is closed' } } });
    renderBoard();

    await drop('PROJ-1', 'In progress');

    expect(await screen.findByText('PROJ-1 is closed')).toBeTruthy();
  });

  it('filters by type and assignee', async () => {
    boardItems = [item(), item({ key: 'PROJ-2', type: 'Task', assignee: 'user-b' })];
    renderBoard();
    await screen.findByText('PROJ-2');

    fireEvent.change(screen.getByLabelText('Type:'), { target: { value: 'Task' } });
    expect(screen.queryByText('PROJ-1')).toBeNull();
    expect(screen.getByText('PROJ-2')).toBeTruthy();

    fireEvent.change(screen.getByLabelText('Type:'), { target: { value: '' } });
    fireEvent.change(screen.getByLabelText('Assignee:'), { target: { value: 'user-a' } });
    expect(screen.getByText('PROJ-1')).toBeTruthy();
    expect(screen.queryByText('PROJ-2')).toBeNull();
  });
});
