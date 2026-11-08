import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import RequireAdmin from './components/RequireAdmin';
import PlannedPage from './pages/PlannedPage';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/my-work" replace />} />
        <Route path="/my-work" element={<PlannedPage title="My work" />} />
        <Route path="/items" element={<PlannedPage title="Items" />} />
        <Route path="/items/:key" element={<PlannedPage title="Item" />} />
        <Route path="/board" element={<PlannedPage title="Board" />} />
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
