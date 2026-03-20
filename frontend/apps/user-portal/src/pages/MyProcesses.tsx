import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@wfp/shared-ui';
import type { ProcessInstance } from '@wfp/shared-ui';

export default function MyProcesses() {
  const { data, isLoading } = useQuery({
    queryKey: ['my-processes'],
    queryFn: () => apiClient.get('/api/workflow/processes').then((r) => r.data),
  });

  return (
    <div>
      <h1 style={{ marginBottom: '1rem' }}>My Processes</h1>
      {isLoading ? <p>Loading...</p> : (
        <table style={{ width: '100%', background: '#fff', borderCollapse: 'collapse', borderRadius: 8 }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #eee' }}>
              <th style={{ textAlign: 'left', padding: '0.75rem' }}>Process</th>
              <th style={{ textAlign: 'left', padding: '0.75rem' }}>Business Key</th>
              <th style={{ textAlign: 'left', padding: '0.75rem' }}>Started</th>
              <th style={{ textAlign: 'left', padding: '0.75rem' }}>Status</th>
            </tr>
          </thead>
          <tbody>
            {(data?.content ?? []).map((p: ProcessInstance) => (
              <tr key={p.id} style={{ borderBottom: '1px solid #eee' }}>
                <td style={{ padding: '0.75rem' }}>{p.processDefinitionName || p.processDefinitionKey}</td>
                <td style={{ padding: '0.75rem' }}>{p.businessKey || '--'}</td>
                <td style={{ padding: '0.75rem', fontSize: '0.85rem' }}>{new Date(p.startTime).toLocaleString()}</td>
                <td style={{ padding: '0.75rem' }}>
                  <span style={{ background: '#e8f5e9', color: '#2e7d32', padding: '0.2rem 0.5rem', borderRadius: 4, fontSize: '0.85rem' }}>Running</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
