import { useQuery } from '@tanstack/react-query';
import { apiClient, useAuth } from '@wfp/shared-ui';
import { Link } from 'react-router-dom';
import type { Task } from '@wfp/shared-ui';

export default function TaskInbox() {
  const { username } = useAuth();

  const { data, isLoading } = useQuery({
    queryKey: ['tasks', username],
    queryFn: () => apiClient.get(`/workflow/tasks?assignee=${username}`).then((r) => r.data),
  });

  return (
    <div>
      <h1 style={{ marginBottom: '1rem' }}>My Tasks</h1>
      {isLoading ? <p>Loading...</p> : (
        <table style={{ width: '100%', background: '#fff', borderCollapse: 'collapse', borderRadius: 8 }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #eee' }}>
              <th style={{ textAlign: 'left', padding: '0.75rem' }}>Task</th>
              <th style={{ textAlign: 'left', padding: '0.75rem' }}>Process</th>
              <th style={{ textAlign: 'left', padding: '0.75rem' }}>Created</th>
              <th style={{ textAlign: 'left', padding: '0.75rem' }}>Priority</th>
              <th style={{ textAlign: 'left', padding: '0.75rem' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {(data?.content ?? []).map((task: Task) => (
              <tr key={task.id} style={{ borderBottom: '1px solid #eee' }}>
                <td style={{ padding: '0.75rem' }}><Link to={`/tasks/${task.id}`} style={{ color: '#1976d2' }}>{task.name}</Link></td>
                <td style={{ padding: '0.75rem' }}>{task.processDefinitionKey}</td>
                <td style={{ padding: '0.75rem', fontSize: '0.85rem' }}>{new Date(task.createTime).toLocaleDateString()}</td>
                <td style={{ padding: '0.75rem' }}>{task.priority}</td>
                <td style={{ padding: '0.75rem' }}><Link to={`/tasks/${task.id}`} style={{ color: '#1976d2' }}>Open</Link></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
