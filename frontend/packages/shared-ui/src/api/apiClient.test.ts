import type { AxiosAdapter, InternalAxiosRequestConfig } from 'axios';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { apiClient, setTokenProvider } from './apiClient';

function respondWith(status: number) {
  const sent: InternalAxiosRequestConfig[] = [];
  const adapter: AxiosAdapter = async (config) => {
    sent.push(config);
    const response = { data: {}, status, statusText: '', headers: {}, config };
    if (status >= 400) {
      throw Object.assign(new Error(`Request failed with status ${status}`), { response, config });
    }
    return response;
  };
  apiClient.defaults.adapter = adapter;
  return sent;
}

describe('apiClient', () => {
  const reload = vi.fn();

  beforeEach(() => {
    vi.stubGlobal('location', { ...window.location, reload });
  });

  afterEach(() => {
    setTokenProvider(() => undefined);
    reload.mockReset();
    vi.unstubAllGlobals();
  });

  it('sends the bearer token from the token provider', async () => {
    const sent = respondWith(200);
    setTokenProvider(() => 'token-123');

    await apiClient.get('/items');

    expect(sent[0].headers.Authorization).toBe('Bearer token-123');
    expect(sent[0].baseURL).toBe('/api');
  });

  it('sends no Authorization header without a token', async () => {
    const sent = respondWith(200);

    await apiClient.get('/items');

    expect(sent[0].headers.Authorization).toBeUndefined();
  });

  it('reloads the page on a 401 when a token was set, so the user logs in again', async () => {
    respondWith(401);
    setTokenProvider(() => 'expired-token');

    await expect(apiClient.get('/items')).rejects.toMatchObject({ response: { status: 401 } });

    expect(reload).toHaveBeenCalledOnce();
  });

  it('does not reload on a 401 before a token is set', async () => {
    respondWith(401);

    await expect(apiClient.get('/items')).rejects.toMatchObject({ response: { status: 401 } });

    expect(reload).not.toHaveBeenCalled();
  });

  it('rejects other errors unchanged', async () => {
    respondWith(500);
    setTokenProvider(() => 'token-123');

    await expect(apiClient.get('/items')).rejects.toMatchObject({ response: { status: 500 } });

    expect(reload).not.toHaveBeenCalled();
  });
});
