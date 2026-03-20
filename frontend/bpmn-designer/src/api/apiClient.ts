import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from "axios";
import { UserManager } from "oidc-client-ts";
import { oidcConfig } from "@/auth/authConfig";
import { getToken } from "./tokenStore";

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
    _userManager = new UserManager(oidcConfig);
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

// Response interceptor: on 401 attempt silent refresh then redirect
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      try {
        await getUserManager().signinSilent();
      } catch {
        await getUserManager().signinRedirect();
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
