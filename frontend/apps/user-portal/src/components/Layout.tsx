import { Link, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '@wfp/shared-ui';

const navItems = [
  { path: '/dashboard', label: 'Dashboard' },
  { path: '/tasks', label: 'My Tasks' },
  { path: '/start-process', label: 'Start Process' },
  { path: '/my-processes', label: 'My Processes' },
  { path: '/notifications', label: 'Notifications' },
];

export default function Layout() {
  const { username, logout } = useAuth();
  const location = useLocation();

  return (
    <div style={{ display: 'flex', height: '100vh' }}>
      <nav style={{ width: 220, background: '#0d47a1', color: '#fff', padding: '1rem 0', display: 'flex', flexDirection: 'column' }}>
        <h2 style={{ padding: '0 1rem', marginBottom: '2rem', fontSize: '1.2rem' }}>WFP Portal</h2>
        {navItems.map((item) => (
          <Link
            key={item.path}
            to={item.path}
            style={{
              padding: '0.75rem 1rem',
              color: location.pathname.startsWith(item.path) ? '#bbdefb' : '#ccc',
              textDecoration: 'none',
              background: location.pathname.startsWith(item.path) ? 'rgba(255,255,255,0.1)' : 'transparent',
            }}
          >
            {item.label}
          </Link>
        ))}
        <div style={{ marginTop: 'auto', padding: '1rem', borderTop: '1px solid rgba(255,255,255,0.2)' }}>
          <div style={{ fontSize: '0.85rem', marginBottom: '0.5rem' }}>{username}</div>
          <button onClick={logout} style={{ background: 'none', border: '1px solid rgba(255,255,255,0.4)', color: '#ccc', padding: '0.3rem 0.8rem', cursor: 'pointer', borderRadius: 4 }}>
            Logout
          </button>
        </div>
      </nav>
      <main style={{ flex: 1, overflow: 'auto', padding: '1.5rem', background: '#f5f5f5' }}>
        <Outlet />
      </main>
    </div>
  );
}
