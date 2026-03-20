import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import TaskInbox from './pages/TaskInbox';
import TaskDetail from './pages/TaskDetail';
import StartProcess from './pages/StartProcess';
import MyProcesses from './pages/MyProcesses';
import Notifications from './pages/Notifications';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/tasks" element={<TaskInbox />} />
        <Route path="/tasks/:id" element={<TaskDetail />} />
        <Route path="/start-process" element={<StartProcess />} />
        <Route path="/my-processes" element={<MyProcesses />} />
        <Route path="/notifications" element={<Notifications />} />
      </Route>
    </Routes>
  );
}
