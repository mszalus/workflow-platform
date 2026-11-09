import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import { ProjectProvider, type Project } from './projects/ProjectContext';

const auth = vi.hoisted(() => ({ roles: [] as string[] }));
const api = vi.hoisted(() => ({ get: vi.fn(), put: vi.fn() }));

vi.mock('./shared-ui', () => ({
  useAuth: () => ({ username: 'user-a', tenantId: 'tenant-a', roles: auth.roles, logout: vi.fn() }),
  apiClient: api,
}));
vi.mock('./components/ItemDiagram', () => ({ default: () => null }));

const projects: Project[] = [
  { key: 'PROJ', name: 'Platform', itemTypes: [] },
  { key: 'OPS', name: 'Operations', itemTypes: [] },
];

const itemNotification = {
  id: 'n1',
  title: 'PROJ-1 assigned to you',
  message: 'Fix login',
  read: false,
  referenceId: 'PROJ-1',
  referenceType: 'ITEM',
};

const openedItem = {
  key: 'PROJ-1',
  title: 'Fix login',
  priority: 'LOW',
  reporter: 'user-b',
  statusName: 'Open',
  fields: {},
  createdAt: '2026-11-01T10:00:00Z',
  updatedAt: '2026-11-01T10:00:00Z',
};

function renderApp(path = '/') {
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
  auth.roles = ['user'];
  localStorage.clear();
  api.put.mockReset().mockResolvedValue({ data: undefined });
  api.get.mockReset().mockImplementation((url: string) => {
    if (url === '/notifications/unread-count') return Promise.resolve({ data: { count: 1 } });
    if (url === '/notifications') return Promise.resolve({ data: { content: [itemNotification] } });
    if (url === '/items') return Promise.resolve({ data: { content: [] } });
    if (url === '/items/PROJ-1') return Promise.resolve({ data: openedItem });
    if (
      ['/items/PROJ-1/comments', '/items/PROJ-1/history', '/items/PROJ-1/attachments', '/fields/schemas'].includes(url)
    ) {
      return Promise.resolve({ data: [] });
    }
    return Promise.resolve({ data: projects });
  });
});

describe('navigation', () => {
  it('opens on My work and shows who is signed in', () => {
    renderApp();

    expect(screen.getByRole('heading', { name: 'My work' })).toBeTruthy();
    expect(screen.getByText('user-a · tenant-a')).toBeTruthy();
  });

  it('hides the Admin section from a user', () => {
    renderApp();

    expect(screen.queryByText('ADMIN')).toBeNull();
    expect(screen.queryByRole('link', { name: 'Projects' })).toBeNull();
  });

  it('shows the Admin section to an admin', () => {
    auth.roles = ['admin'];
    renderApp();

    expect(screen.getByText('ADMIN')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Audit log' })).toBeTruthy();
  });

  it('sends a user away from an admin page', () => {
    renderApp('/admin/projects');

    expect(screen.getByRole('heading', { name: 'My work' })).toBeTruthy();
  });

  it('lets an admin open an admin page', () => {
    auth.roles = ['admin'];
    renderApp('/admin/projects');

    expect(screen.getByRole('heading', { name: 'Projects' })).toBeTruthy();
  });
});

describe('project switcher', () => {
  const selectedProject = () => (screen.getByLabelText('Project') as HTMLSelectElement).value;

  it('starts on the first project', () => {
    renderApp();

    expect(selectedProject()).toBe('PROJ');
  });

  it('remembers the chosen project', () => {
    const { unmount } = renderApp();
    fireEvent.change(screen.getByLabelText('Project'), { target: { value: 'OPS' } });
    expect(selectedProject()).toBe('OPS');
    unmount();

    renderApp();

    expect(selectedProject()).toBe('OPS');
  });
});

describe('notification bell', () => {
  const bell = () => screen.findByRole('button', { name: 'Notifications, 1 unread' });

  it('shows the unread count', async () => {
    renderApp();

    expect(await bell()).toBeTruthy();
  });

  it('marks a notification read and opens its item', async () => {
    renderApp();
    fireEvent.click(await bell());

    fireEvent.click(await screen.findByText('PROJ-1 assigned to you'));

    await waitFor(() => expect(api.put).toHaveBeenCalledWith('/notifications/mark-read', ['n1']));
    expect(await screen.findByRole('heading', { name: 'Fix login' })).toBeTruthy();
  });

  it('marks everything read', async () => {
    renderApp();
    fireEvent.click(await bell());

    fireEvent.click(await screen.findByText('Mark all read'));

    await waitFor(() => expect(api.put).toHaveBeenCalledWith('/notifications/mark-all-read'));
  });
});
