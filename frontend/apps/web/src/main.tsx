import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@wfp/shared-ui';
import App from './App';
import { ProjectProvider } from './projects/ProjectContext';

const queryClient = new QueryClient();

const keycloakUrl =
  window.location.hostname === 'localhost' ? 'http://localhost:8180' : `http://${window.location.hostname}:8180`;

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AuthProvider keycloakUrl={keycloakUrl} realm="workflow-platform" clientId="wfp-user-portal">
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
