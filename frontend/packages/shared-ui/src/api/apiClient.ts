import axios from 'axios';

export const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

let getToken: (() => string | undefined) | null = null;

export function setTokenProvider(provider: () => string | undefined) {
  getToken = provider;
}

apiClient.interceptors.request.use((config) => {
  const token = getToken?.();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && getToken?.()) {
      // Only reload if we had a token (it expired) — avoid loop when token isn't set yet
      window.location.reload();
    }
    return Promise.reject(error);
  }
);
