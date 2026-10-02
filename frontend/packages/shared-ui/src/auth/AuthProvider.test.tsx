import { act, render, screen } from '@testing-library/react';
import type { AxiosAdapter, InternalAxiosRequestConfig } from 'axios';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { apiClient } from '../api/apiClient';
import { AuthProvider, useAuth } from './AuthProvider';

const keycloak = vi.hoisted(() => ({
  init: vi.fn(),
  updateToken: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  token: undefined as string | undefined,
  tokenParsed: undefined as Record<string, unknown> | undefined,
  onTokenExpired: undefined as (() => void) | undefined,
}));

vi.mock('keycloak-js', () => ({
  default: function Keycloak() {
    return keycloak;
  },
}));

function Identity() {
  const { authenticated, tenantId, username, userId, roles } = useAuth();
  return <pre data-testid="identity">{JSON.stringify({ authenticated, tenantId, username, userId, roles })}</pre>;
}

function renderProvider() {
  return render(
    <AuthProvider keycloakUrl="http://keycloak" realm="workflow-platform" clientId="portal">
      <Identity />
    </AuthProvider>,
  );
}

async function sentAuthorizationHeader() {
  const sent: InternalAxiosRequestConfig[] = [];
  const adapter: AxiosAdapter = async (config) => {
    sent.push(config);
    return { data: {}, status: 200, statusText: '', headers: {}, config };
  };
  apiClient.defaults.adapter = adapter;
  await apiClient.get('/items');
  return sent[0].headers.Authorization;
}

describe('AuthProvider', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    keycloak.token = 'token-1';
    keycloak.tokenParsed = {
      sub: 'user-1',
      tenant_id: 'tenant-a',
      preferred_username: 'alice',
      realm_access: { roles: ['user'] },
    };
    keycloak.init.mockResolvedValue(true);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('shows a loading state until Keycloak is initialised', async () => {
    let finishInit: (authenticated: boolean) => void = () => {};
    keycloak.init.mockReturnValue(new Promise<boolean>((resolve) => (finishInit = resolve)));

    renderProvider();
    expect(screen.getByText('Loading...')).toBeTruthy();

    await act(async () => finishInit(true));
    expect(screen.getByTestId('identity')).toBeTruthy();
  });

  it('logs in with PKCE and exposes the identity from the token', async () => {
    renderProvider();

    expect(JSON.parse((await screen.findByTestId('identity')).textContent ?? '')).toEqual({
      authenticated: true,
      tenantId: 'tenant-a',
      username: 'alice',
      userId: 'user-1',
      roles: ['user'],
    });
    expect(keycloak.init).toHaveBeenCalledWith(
      expect.objectContaining({ onLoad: 'login-required', pkceMethod: 'S256' }),
    );
  });

  it('gives apiClient the token once authenticated', async () => {
    renderProvider();
    await screen.findByTestId('identity');

    expect(await sentAuthorizationHeader()).toBe('Bearer token-1');
  });

  it('refreshes an expired token', async () => {
    keycloak.updateToken.mockResolvedValue(true);
    renderProvider();
    await screen.findByTestId('identity');

    keycloak.token = 'token-2';
    await act(async () => keycloak.onTokenExpired?.());

    expect(keycloak.updateToken).toHaveBeenCalledWith(30);
    expect(keycloak.login).not.toHaveBeenCalled();
    expect(await sentAuthorizationHeader()).toBe('Bearer token-2');
  });

  it('sends the user to login when the refresh fails', async () => {
    keycloak.updateToken.mockRejectedValue(new Error('refresh token expired'));
    renderProvider();
    await screen.findByTestId('identity');

    await act(async () => keycloak.onTokenExpired?.());

    expect(keycloak.login).toHaveBeenCalledOnce();
  });

  it('refuses useAuth outside the provider', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});

    expect(() => render(<Identity />)).toThrow('useAuth must be used within an AuthProvider');
  });
});
