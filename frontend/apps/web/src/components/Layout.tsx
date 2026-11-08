import { useState, type CSSProperties } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '@wfp/shared-ui';
import { keycloakUsersUrl } from '../auth/keycloakUrl';
import { useIsAdmin } from '../auth/useIsAdmin';
import CreateItemDialog from './CreateItemDialog';
import NotificationBell from './NotificationBell';
import ProjectSwitcher from './ProjectSwitcher';

const userLinks = [
  { to: '/my-work', label: 'My work' },
  { to: '/items', label: 'Items' },
  { to: '/board', label: 'Board' },
];

const adminLinks = [
  { to: '/admin/projects', label: 'Projects' },
  { to: '/admin/workflows', label: 'Workflows' },
  { to: '/admin/fields', label: 'Fields' },
  { to: '/admin/audit', label: 'Audit log' },
];

function linkStyle({ isActive }: { isActive: boolean }): CSSProperties {
  return {
    display: 'block',
    padding: '0.6rem 1rem',
    color: isActive ? '#bbdefb' : '#ccc',
    textDecoration: 'none',
    background: isActive ? 'rgba(255,255,255,0.1)' : 'transparent',
  };
}

function NavLinks({ links }: { links: { to: string; label: string }[] }) {
  return links.map((link) => (
    <NavLink key={link.to} to={link.to} style={linkStyle}>
      {link.label}
    </NavLink>
  ));
}

export default function Layout() {
  const { username, tenantId, logout } = useAuth();
  const isAdmin = useIsAdmin();
  const [creating, setCreating] = useState(false);

  return (
    <div style={{ display: 'flex', height: '100vh' }}>
      <nav
        style={{
          width: 220,
          background: '#0d47a1',
          color: '#fff',
          padding: '1rem 0',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <div style={{ padding: '0 1rem', marginBottom: '1.5rem' }}>
          <h2 style={{ fontSize: '1.2rem', margin: '0 0 0.5rem' }}>WFP</h2>
          <ProjectSwitcher />
        </div>
        <NavLinks links={userLinks} />
        {isAdmin && (
          <>
            <h3 style={{ fontSize: '0.75rem', margin: '1.5rem 1rem 0.5rem', color: '#bbdefb' }}>ADMIN</h3>
            <NavLinks links={adminLinks} />
            <a href={keycloakUsersUrl} target="_blank" rel="noreferrer" style={linkStyle({ isActive: false })}>
              Users and groups
            </a>
          </>
        )}
        <div style={{ marginTop: 'auto', padding: '1rem', borderTop: '1px solid rgba(255,255,255,0.2)' }}>
          <div style={{ fontSize: '0.85rem', marginBottom: '0.5rem' }}>
            {username} · {tenantId}
          </div>
          <button onClick={logout}>Log out</button>
        </div>
      </nav>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: '#f5f5f5' }}>
        <header style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', padding: '0.75rem 1.5rem' }}>
          <NotificationBell />
          <button onClick={() => setCreating(true)}>+ Create</button>
        </header>
        <main style={{ flex: 1, overflow: 'auto', padding: '0 1.5rem 1.5rem' }}>
          <Outlet />
        </main>
      </div>
      {creating && <CreateItemDialog onClose={() => setCreating(false)} />}
    </div>
  );
}
