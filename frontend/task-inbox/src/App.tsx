import { useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { Spinner } from "@workflow/ui-common";
import { setToken } from "@/api/tokenStore";
import TaskInboxPage from "@/pages/TaskInboxPage";
import TaskDetailPage from "@/pages/TaskDetailPage";
import NotificationsPage from "@/pages/NotificationsPage";

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
          <Route path="/" element={<Navigate to="/tasks" replace />} />
          <Route path="/tasks" element={<TaskInboxPage />} />
          <Route path="/tasks/:id" element={<TaskDetailPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="*" element={<Navigate to="/tasks" replace />} />
        </Routes>
      </AuthGuard>
    </BrowserRouter>
  );
}
