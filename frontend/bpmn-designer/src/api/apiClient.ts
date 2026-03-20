import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from "axios";
import { UserManager, type UserManagerSettings } from "oidc-client-ts";
import { oidcConfig } from "@/auth/authConfig";
import { getToken, setToken } from "./tokenStore";

const apiGatewayUrl = import.meta.env.VITE_API_GATEWAY_URL as string | undefined;

export const apiClient: AxiosInstance = axios.create({
  baseURL: apiGatewayUrl ?? "",
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 30_000,
});

// Lazy singleton user manager used only for silent renew / redirect on 401
let _userManager: UserManager | null = null;
function getUserManager(): UserManager {
  if (!_userManager) {
    _userManager = new UserManager(oidcConfig as UserManagerSettings);
  }
  return _userManager;
}

// Request interceptor: attach Bearer token from the token store
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Refresh lock to prevent concurrent silent renew attempts
let refreshPromise: Promise<void> | null = null;

// Response interceptor: on 401 attempt silent refresh then redirect
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      if (!refreshPromise) {
        refreshPromise = getUserManager()
          .signinSilent()
          .then((user) => {
            if (user?.access_token) setToken(user.access_token);
          })
          .catch(() => {
            getUserManager().signinRedirect();
          })
          .finally(() => {
            refreshPromise = null;
          });
      }
      await refreshPromise;
      return apiClient(originalRequest);
    }
    return Promise.reject(error);
  }
);

export default apiClient;
