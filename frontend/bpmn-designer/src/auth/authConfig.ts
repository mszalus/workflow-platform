import type { AuthProviderProps } from "react-oidc-context";

const keycloakUrl = import.meta.env.VITE_KEYCLOAK_URL as string;
const keycloakRealm = import.meta.env.VITE_KEYCLOAK_REALM as string;
const keycloakClientId = import.meta.env.VITE_KEYCLOAK_CLIENT_ID as string;

if (!keycloakUrl || !keycloakRealm || !keycloakClientId) {
  console.warn(
    "Missing OIDC environment variables. Expected: VITE_KEYCLOAK_URL, VITE_KEYCLOAK_REALM, VITE_KEYCLOAK_CLIENT_ID"
  );
}

export const oidcConfig: AuthProviderProps = {
  authority: `${keycloakUrl}/realms/${keycloakRealm}`,
  client_id: keycloakClientId,
  redirect_uri: window.location.origin,
  post_logout_redirect_uri: window.location.origin,
  response_type: "code",
  scope: "openid profile email",
  automaticSilentRenew: true,
  loadUserInfo: true,
  onSigninCallback: () => {
    // Remove OIDC params from URL after successful login
    window.history.replaceState({}, document.title, window.location.pathname);
  },
};
