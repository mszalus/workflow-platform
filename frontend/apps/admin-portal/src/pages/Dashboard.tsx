import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@wfp/shared-ui';

export default function Dashboard() {
  const { data: processes } = useQuery({
    queryKey: ['admin-processes'],
    queryFn: () => apiClient.get('/workflow/deployments').then((r) => r.data),
  });

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem' }}>Dashboard</h1>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1rem' }}>
        <div style={{ background: '#fff', padding: '1.5rem', borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
          <h3 style={{ color: '#666', fontSize: '0.85rem', textTransform: 'uppercase' }}>Deployed Processes</h3>
          <div style={{ fontSize: '2rem', fontWeight: 'bold', marginTop: '0.5rem' }}>{processes?.length ?? 0}</div>
        </div>
        <div style={{ background: '#fff', padding: '1.5rem', borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
          <h3 style={{ color: '#666', fontSize: '0.85rem', textTransform: 'uppercase' }}>Active Instances</h3>
          <div style={{ fontSize: '2rem', fontWeight: 'bold', marginTop: '0.5rem' }}>--</div>
        </div>
        <div style={{ background: '#fff', padding: '1.5rem', borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
          <h3 style={{ color: '#666', fontSize: '0.85rem', textTransform: 'uppercase' }}>Custom Field Schemas</h3>
          <div style={{ fontSize: '2rem', fontWeight: 'bold', marginTop: '0.5rem' }}>--</div>
        </div>
      </div>
    </div>
  );
}
