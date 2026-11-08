import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import RequireAdmin from './components/RequireAdmin';
import BoardPage from './pages/BoardPage';
import ItemDetailPage from './pages/ItemDetailPage';
import ItemsPage from './pages/ItemsPage';
import MyWorkPage from './pages/MyWorkPage';
import PlannedPage from './pages/PlannedPage';

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
          <Route path="projects" element={<PlannedPage title="Projects" />} />
          <Route path="workflows" element={<PlannedPage title="Workflows" />} />
          <Route path="fields" element={<PlannedPage title="Fields" />} />
          <Route path="audit" element={<PlannedPage title="Audit log" />} />
        </Route>
      </Route>
    </Routes>
  );
}
