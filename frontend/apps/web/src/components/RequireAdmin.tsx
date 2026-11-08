import { Navigate, Outlet } from 'react-router-dom';
import { useIsAdmin } from '../auth/useIsAdmin';

export default function RequireAdmin() {
  return useIsAdmin() ? <Outlet /> : <Navigate to="/my-work" replace />;
}
