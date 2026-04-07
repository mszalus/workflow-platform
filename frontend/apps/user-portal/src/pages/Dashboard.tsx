import { useQuery } from '@tanstack/react-query';
import { apiClient, useAuth } from '@wfp/shared-ui';
import { Link } from 'react-router-dom';

export default function Dashboard() {
  const { username } = useAuth();

  const { data: tasks } = useQuery({
    queryKey: ['my-tasks'],
    queryFn: () => apiClient.get(`/workflow/tasks?assignee=${username}&size=5`).then((r) => r.data),
  });

  const { data: unread } = useQuery({
    queryKey: ['unread-count'],
    queryFn: () => apiClient.get('/notifications/unread-count').then((r) => r.data),
  });

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem' }}>Welcome, {username}</h1>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1rem', marginBottom: '2rem' }}>
        <div style={{ background: '#fff', padding: '1.5rem', borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
          <h3 style={{ color: '#666', fontSize: '0.85rem', textTransform: 'uppercase' }}>My Tasks</h3>
          <div style={{ fontSize: '2rem', fontWeight: 'bold', marginTop: '0.5rem' }}>{tasks?.totalElements ?? 0}</div>
        </div>
        <div style={{ background: '#fff', padding: '1.5rem', borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
          <h3 style={{ color: '#666', fontSize: '0.85rem', textTransform: 'uppercase' }}>Unread Notifications</h3>
          <div style={{ fontSize: '2rem', fontWeight: 'bold', marginTop: '0.5rem' }}>{unread?.count ?? 0}</div>
        </div>
        <Link
          to="/start-process"
          style={{
            background: '#1976d2',
            color: '#fff',
            padding: '1.5rem',
            borderRadius: 8,
            textDecoration: 'none',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '1.1rem',
          }}
        >
          Start New Process
        </Link>
      </div>
    </div>
  );
}
