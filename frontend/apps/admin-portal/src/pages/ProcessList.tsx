import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@wfp/shared-ui';
import { Link } from 'react-router-dom';

export default function ProcessList() {
  const queryClient = useQueryClient();
  const { data: processes = [], isLoading } = useQuery({
    queryKey: ['process-definitions'],
    queryFn: () => apiClient.get('/api/workflow/deployments').then((r) => r.data),
  });

  const deleteMutation = useMutation({
    mutationFn: (deploymentId: string) => apiClient.delete(`/api/workflow/deployments/${deploymentId}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['process-definitions'] }),
  });

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <h1>Process Definitions</h1>
        <Link to="/processes/designer" style={{ background: '#1976d2', color: '#fff', padding: '0.5rem 1rem', borderRadius: 4, textDecoration: 'none' }}>
          New Process
        </Link>
      </div>
      {isLoading ? <p>Loading...</p> : (
        <table style={{ width: '100%', background: '#fff', borderCollapse: 'collapse', borderRadius: 8 }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #eee' }}>
              <th style={{ textAlign: 'left', padding: '0.75rem' }}>Name</th>
              <th style={{ textAlign: 'left', padding: '0.75rem' }}>Key</th>
              <th style={{ textAlign: 'left', padding: '0.75rem' }}>Version</th>
              <th style={{ textAlign: 'left', padding: '0.75rem' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {processes.map((p: any) => (
              <tr key={p.id} style={{ borderBottom: '1px solid #eee' }}>
                <td style={{ padding: '0.75rem' }}>{p.name}</td>
                <td style={{ padding: '0.75rem' }}>{p.key}</td>
                <td style={{ padding: '0.75rem' }}>{p.version}</td>
                <td style={{ padding: '0.75rem' }}>
                  <button onClick={() => deleteMutation.mutate(p.deploymentId)} style={{ color: '#d32f2f', background: 'none', border: 'none', cursor: 'pointer' }}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
