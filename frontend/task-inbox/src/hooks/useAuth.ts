import { useAuth as useOidcAuth } from "react-oidc-context";

export interface AuthUser {
  id: string;
  email: string;
  name: string;
  tenantId: string;
  roles: string[];
  accessToken: string;
}

export function useAuth() {
  const auth = useOidcAuth();
  const user = auth.user;
  const profile = user?.profile;

  const tenantId =
    (profile?.["tenant_id"] as string | undefined) ??
    (profile?.["tenantId"] as string | undefined) ??
    "";

  const realmAccess = profile?.["realm_access"] as { roles?: string[] } | undefined;
  const roles: string[] = realmAccess?.roles ?? [];

  const authUser: AuthUser | null = user
    ? {
        id: profile?.sub ?? "",
        email: (profile?.email as string | undefined) ?? "",
        name: profile?.name ?? profile?.preferred_username ?? "",
        tenantId,
        roles,
        accessToken: user.access_token ?? "",
      }
    : null;

  return {
    user: authUser,
    isAuthenticated: auth.isAuthenticated,
    isLoading: auth.isLoading,
    error: auth.error,
    signIn: () => auth.signinRedirect(),
    signOut: () => auth.signoutRedirect(),
    hasRole: (role: string) => roles.includes(role),
  };
}
