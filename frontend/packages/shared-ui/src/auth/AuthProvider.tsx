import React, { createContext, useContext, useEffect, useState, useCallback, type ReactNode } from 'react';
import Keycloak from 'keycloak-js';

interface AuthContextType {
  keycloak: Keycloak | null;
  initialized: boolean;
  authenticated: boolean;
  token: string | undefined;
  tenantId: string | undefined;
  userId: string | undefined;
  username: string | undefined;
  roles: string[];
  login: () => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
  keycloakUrl: string;
  realm: string;
  clientId: string;
}

export function AuthProvider({ children, keycloakUrl, realm, clientId }: AuthProviderProps) {
  const [keycloak] = useState(
    () =>
      new Keycloak({
        url: keycloakUrl,
        realm,
        clientId,
      })
  );
  const [initialized, setInitialized] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);

  useEffect(() => {
    keycloak
      .init({
        onLoad: 'login-required',
        checkLoginIframe: false,
        pkceMethod: 'S256',
      })
      .then((auth) => {
        setAuthenticated(auth);
        setInitialized(true);
      });

    keycloak.onTokenExpired = () => {
      keycloak.updateToken(30).catch(() => keycloak.login());
    };
  }, [keycloak]);

  const login = useCallback(() => keycloak.login(), [keycloak]);
  const logout = useCallback(() => keycloak.logout(), [keycloak]);

  const tokenParsed = keycloak.tokenParsed;
  const value: AuthContextType = {
    keycloak,
    initialized,
    authenticated,
    token: keycloak.token,
    tenantId: tokenParsed?.tenant_id as string | undefined,
    userId: tokenParsed?.sub,
    username: tokenParsed?.preferred_username as string | undefined,
    roles: (tokenParsed?.realm_access?.roles as string[]) ?? [],
    login,
    logout,
  };

  if (!initialized) {
    return <div>Loading...</div>;
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
