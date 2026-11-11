import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import RequireOrganization from './RequireOrganization';

const auth = vi.hoisted(() => ({ tenantId: undefined as string | undefined, logout: vi.fn() }));
const api = vi.hoisted(() => ({ get: vi.fn() }));

vi.mock('../shared-ui', () => ({
  useAuth: () => ({ username: 'user-a', roles: [], ...auth }),
  apiClient: api,
}));

beforeEach(() => {
  auth.tenantId = undefined;
  auth.logout.mockReset();
  api.get.mockReset().mockResolvedValue({ data: [] });
});

describe('RequireOrganization', () => {
  it('shows the message instead of the app and makes no API calls without a tenant', () => {
    render(
      <RequireOrganization>
        <ApiCaller />
      </RequireOrganization>,
    );

    expect(
      screen.getByText("Your account isn't assigned to an organization. Contact your administrator."),
    ).toBeTruthy();
    expect(screen.getByText(/user-a/)).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Log out' }));
    expect(auth.logout).toHaveBeenCalled();
    expect(api.get).not.toHaveBeenCalled();
  });

  it('renders the app with a tenant', () => {
    auth.tenantId = 'tenant-a';
    render(
      <RequireOrganization>
        <p>the app</p>
      </RequireOrganization>,
    );

    expect(screen.getByText('the app')).toBeTruthy();
  });
});

function ApiCaller() {
  void api.get('/projects');
  return null;
}
