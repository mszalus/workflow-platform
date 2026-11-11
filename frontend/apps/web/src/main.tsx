import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './shared-ui';
import App from './App';
import { keycloakUrl } from './auth/keycloakUrl';
import { ProjectProvider } from './projects/ProjectContext';
import RequireOrganization from './components/RequireOrganization';
import { ThemeProvider } from './theme';

const queryClient = new QueryClient();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ThemeProvider>
      <AuthProvider keycloakUrl={keycloakUrl} realm="workflow-platform" clientId="wfp-web">
        <RequireOrganization>
          <QueryClientProvider client={queryClient}>
            <ProjectProvider>
              <BrowserRouter>
                <App />
              </BrowserRouter>
            </ProjectProvider>
          </QueryClientProvider>
        </RequireOrganization>
      </AuthProvider>
    </ThemeProvider>
  </React.StrictMode>,
);
