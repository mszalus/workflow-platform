import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient, useAuth } from '@wfp/shared-ui';
import { Link } from 'react-router-dom';
import type { Task } from '@wfp/shared-ui';

export default function TaskInbox() {
  const { username } = useAuth();
  const queryClient = useQueryClient();

  // Fetch tasks assigned to the current user
  const { data, isLoading } = useQuery({
    queryKey: ['tasks', username],
    queryFn: () => apiClient.get(`/workflow/tasks?assignee=${username}`).then((r) => r.data),
  });

  // Fetch unassigned candidate-group tasks the user can claim
  const { data: candidateData } = useQuery({
    queryKey: ['candidate-tasks'],
    queryFn: () => apiClient.get('/workflow/tasks').then((r) => r.data),
  });

  const claimMutation = useMutation({
    mutationFn: (taskId: string) => apiClient.post(`/workflow/tasks/${taskId}/claim`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      queryClient.invalidateQueries({ queryKey: ['candidate-tasks'] });
    },
  });

  const myTasks: Task[] = data?.content ?? [];
  // Show unassigned tasks that the user could claim
  const claimableTasks: Task[] = (candidateData?.content ?? []).filter(
    (t: Task) => !t.assignee
  );

  return (
    <div>
      <h1 style={{ marginBottom: '1rem' }}>My Tasks</h1>
      {isLoading ? <p>Loading...</p> : (
        <>
          <table style={{ width: '100%', background: '#fff', borderCollapse: 'collapse', borderRadius: 8, marginBottom: '2rem' }}>
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
              {myTasks.length === 0 ? (
                <tr><td colSpan={5} style={{ padding: '1rem', color: '#999', textAlign: 'center' }}>No assigned tasks</td></tr>
              ) : myTasks.map((task: Task) => (
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

          {claimableTasks.length > 0 && (
            <>
              <h2 style={{ marginBottom: '0.5rem' }}>Available to Claim</h2>
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
                  {claimableTasks.map((task: Task) => (
                    <tr key={task.id} style={{ borderBottom: '1px solid #eee' }}>
                      <td style={{ padding: '0.75rem' }}>{task.name}</td>
                      <td style={{ padding: '0.75rem' }}>{task.processDefinitionKey}</td>
                      <td style={{ padding: '0.75rem', fontSize: '0.85rem' }}>{new Date(task.createTime).toLocaleDateString()}</td>
                      <td style={{ padding: '0.75rem' }}>{task.priority}</td>
                      <td style={{ padding: '0.75rem' }}>
                        <button
                          onClick={() => claimMutation.mutate(task.id)}
                          disabled={claimMutation.isPending}
                          style={{ background: '#1976d2', color: '#fff', padding: '0.3rem 0.8rem', border: 'none', borderRadius: 4, cursor: 'pointer' }}
                        >
                          Claim
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </>
          )}
        </>
      )}
    </div>
  );
}
