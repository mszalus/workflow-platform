import { useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { Spinner } from "@workflow/ui-common";
import { setToken } from "@/api/tokenStore";
import ProcessListPage from "@/pages/ProcessListPage";
import BpmnDesignerPage from "@/pages/BpmnDesignerPage";
import DeploymentListPage from "@/pages/DeploymentListPage";
import SchemaBuilderPage from "@/pages/SchemaBuilderPage";
import ProcessMonitoringPage from "@/pages/ProcessMonitoringPage";

function AuthGuard({ children }: { children: React.ReactNode }) {
  const auth = useAuth();

  // Keep the token store in sync with the OIDC user
  useEffect(() => {
    setToken(auth.user?.access_token ?? null);
  }, [auth.user?.access_token]);

  if (auth.isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <Spinner size="lg" className="text-primary" />
      </div>
    );
  }

  if (!auth.isAuthenticated) {
    auth.signinRedirect();
    return null;
  }

  return <>{children}</>;
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthGuard>
        <Routes>
          <Route path="/" element={<Navigate to="/processes" replace />} />
          <Route path="/processes" element={<ProcessListPage />} />
          <Route path="/processes/:id/designer" element={<BpmnDesignerPage />} />
          <Route path="/deployments" element={<DeploymentListPage />} />
          <Route path="/schemas" element={<SchemaBuilderPage />} />
          <Route path="/monitoring" element={<ProcessMonitoringPage />} />
          <Route path="*" element={<Navigate to="/processes" replace />} />
        </Routes>
      </AuthGuard>
    </BrowserRouter>
  );
}
