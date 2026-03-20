import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import ProcessList from './pages/ProcessList';
import ProcessDesigner from './pages/ProcessDesigner';
import CustomFieldEditor from './pages/CustomFieldEditor';
import AuditLog from './pages/AuditLog';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/processes" element={<ProcessList />} />
        <Route path="/processes/designer" element={<ProcessDesigner />} />
        <Route path="/processes/designer/:id" element={<ProcessDesigner />} />
        <Route path="/custom-fields" element={<CustomFieldEditor />} />
        <Route path="/audit" element={<AuditLog />} />
      </Route>
    </Routes>
  );
}
