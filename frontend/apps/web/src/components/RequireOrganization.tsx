import type { ReactNode } from 'react';
import { useAuth } from '../shared-ui';

export default function RequireOrganization({ children }: { children: ReactNode }) {
  const { tenantId, username, logout } = useAuth();
  if (tenantId) return <>{children}</>;
  return (
    <div style={{ padding: '2rem' }}>
      <p>Your account isn't assigned to an organization. Contact your administrator.</p>
      <p>Signed in as {username}</p>
      <button onClick={logout}>Log out</button>
    </div>
  );
}
