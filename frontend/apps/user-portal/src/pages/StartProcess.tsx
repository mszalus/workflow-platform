import { useQuery, useMutation } from '@tanstack/react-query';
import { apiClient } from '@wfp/shared-ui';
import { useNavigate } from 'react-router-dom';

export default function StartProcess() {
  const navigate = useNavigate();

  const { data: processes = [] } = useQuery({
    queryKey: ['available-processes'],
    queryFn: () => apiClient.get('/workflow/deployments').then((r) => r.data),
  });

  const startMutation = useMutation({
    mutationFn: (key: string) => apiClient.post('/workflow/processes', { processDefinitionKey: key }),
    onSuccess: () => navigate('/my-processes'),
  });

  return (
    <div>
      <h1 style={{ marginBottom: '1rem' }}>Start Process</h1>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))', gap: '1rem' }}>
        {processes.map((p: any) => (
          <div
            key={p.id}
            style={{ background: '#fff', padding: '1.5rem', borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}
          >
            <h3 style={{ marginBottom: '0.5rem' }}>{p.name}</h3>
            <p style={{ color: '#666', fontSize: '0.85rem', marginBottom: '1rem' }}>Key: {p.key}</p>
            <button
              onClick={() => startMutation.mutate(p.key)}
              disabled={startMutation.isPending}
              style={{
                background: '#1976d2',
                color: '#fff',
                padding: '0.4rem 1rem',
                border: 'none',
                borderRadius: 4,
                cursor: 'pointer',
              }}
            >
              Start
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
