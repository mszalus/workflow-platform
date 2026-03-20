import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient, useAuth } from '@wfp/shared-ui';
import type { Notification } from '@wfp/shared-ui';

export default function Notifications() {
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['notifications'],
    queryFn: () => apiClient.get('/api/notifications/notifications').then((r) => r.data),
  });

  const markAllReadMutation = useMutation({
    mutationFn: () => apiClient.put('/api/notifications/notifications/mark-all-read'),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  });

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <h1>Notifications</h1>
        <button onClick={() => markAllReadMutation.mutate()} style={{ padding: '0.4rem 1rem', cursor: 'pointer' }}>Mark All Read</button>
      </div>
      {isLoading ? <p>Loading...</p> : (
        <div>
          {(data?.content ?? []).map((n: Notification) => (
            <div key={n.id} style={{
              background: n.read ? '#fff' : '#e3f2fd',
              padding: '1rem',
              marginBottom: '0.5rem',
              borderRadius: 8,
              borderLeft: `4px solid ${n.read ? '#ccc' : '#1976d2'}`,
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <strong>{n.title}</strong>
                <span style={{ color: '#999', fontSize: '0.85rem' }}>{new Date(n.createdAt).toLocaleString()}</span>
              </div>
              <p style={{ color: '#666', marginTop: '0.3rem' }}>{n.message}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
