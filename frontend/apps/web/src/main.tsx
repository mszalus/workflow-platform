import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './shared-ui';
import App from './App';
import { keycloakUrl } from './auth/keycloakUrl';
import { ProjectProvider } from './projects/ProjectContext';

const queryClient = new QueryClient();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AuthProvider keycloakUrl={keycloakUrl} realm="workflow-platform" clientId="wfp-web">
      <QueryClientProvider client={queryClient}>
        <ProjectProvider>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </ProjectProvider>
      </QueryClientProvider>
    </AuthProvider>
  </React.StrictMode>,
);
