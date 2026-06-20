import type { AuthProviderProps } from "react-oidc-context";

const keycloakUrl = import.meta.env.VITE_KEYCLOAK_URL as string;
const keycloakRealm = import.meta.env.VITE_KEYCLOAK_REALM as string;
// Default client ID for task-inbox
const keycloakClientId =
  (import.meta.env.VITE_KEYCLOAK_CLIENT_ID as string | undefined) ?? "task-inbox";

if (!keycloakUrl || !keycloakRealm || !keycloakClientId) {
  throw new Error(
    "Missing required OIDC environment variables. Expected: VITE_KEYCLOAK_URL, VITE_KEYCLOAK_REALM, VITE_KEYCLOAK_CLIENT_ID"
  );
}

export const oidcConfig: AuthProviderProps = {
  authority: `${keycloakUrl}/realms/${keycloakRealm}`,
  client_id: keycloakClientId,
  redirect_uri: window.location.origin,
  post_logout_redirect_uri: window.location.origin,
  silent_redirect_uri: window.location.origin + "/silent-renew.html",
  response_type: "code",
  scope: "openid profile email",
  automaticSilentRenew: true,
  loadUserInfo: true,
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, window.location.pathname);
  },
};
