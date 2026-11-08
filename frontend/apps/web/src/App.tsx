import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import RequireAdmin from './components/RequireAdmin';
import BoardPage from './pages/BoardPage';
import ItemDetailPage from './pages/ItemDetailPage';
import ItemsPage from './pages/ItemsPage';
import MyWorkPage from './pages/MyWorkPage';
import AuditPage from './pages/admin/AuditPage';
import FieldsPage from './pages/admin/FieldsPage';
import ProjectsPage from './pages/admin/ProjectsPage';
import WorkflowsPage from './pages/admin/WorkflowsPage';

const WorkflowEditorPage = lazy(() => import('./pages/admin/WorkflowEditorPage'));

function WorkflowEditor() {
  return (
    <Suspense fallback={<p>Loading...</p>}>
      <WorkflowEditorPage />
    </Suspense>
  );
}

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/my-work" replace />} />
        <Route path="/my-work" element={<MyWorkPage />} />
        <Route path="/items" element={<ItemsPage />} />
        <Route path="/items/:key" element={<ItemDetailPage />} />
        <Route path="/board" element={<BoardPage />} />
        <Route path="/admin" element={<RequireAdmin />}>
          <Route path="projects" element={<ProjectsPage />} />
          <Route path="workflows" element={<WorkflowsPage />} />
          <Route path="workflows/new" element={<WorkflowEditor />} />
          <Route path="workflows/:id/edit" element={<WorkflowEditor />} />
          <Route path="fields" element={<FieldsPage />} />
          <Route path="audit" element={<AuditPage />} />
        </Route>
      </Route>
    </Routes>
  );
}
