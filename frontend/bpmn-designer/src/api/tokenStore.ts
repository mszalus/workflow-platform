const TOKEN_KEY = "access_token";

export function setToken(token: string | null): void {
  if (token) {
    sessionStorage.setItem(TOKEN_KEY, token);
  } else {
    sessionStorage.removeItem(TOKEN_KEY);
  }
}

export function getToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY);
}
