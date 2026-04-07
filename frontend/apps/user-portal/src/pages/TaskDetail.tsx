import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient, useAuth } from '@wfp/shared-ui';
import { useState } from 'react';
import DynamicFieldForm from '../components/DynamicFieldForm';

export default function TaskDetail() {
  const { id } = useParams<{ id: string }>();
  const { username } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [comment, setComment] = useState('');

  const { data: task } = useQuery({
    queryKey: ['task', id],
    queryFn: () => apiClient.get(`/workflow/tasks/${id}`).then((r) => r.data),
  });

  const { data: comments = [] } = useQuery({
    queryKey: ['comments', task?.processInstanceId],
    queryFn: () => apiClient.get(`/workflow/processes/${task.processInstanceId}/comments`).then((r) => r.data),
    enabled: !!task?.processInstanceId,
  });

  const completeMutation = useMutation({
    mutationFn: () => apiClient.post(`/workflow/tasks/${id}/complete`, {}),
    onSuccess: () => navigate('/tasks'),
  });

  const addCommentMutation = useMutation({
    mutationFn: (content: string) =>
      apiClient.post(`/workflow/processes/${task.processInstanceId}/comments`, { content }),
    onSuccess: () => {
      setComment('');
      queryClient.invalidateQueries({ queryKey: ['comments', task?.processInstanceId] });
    },
  });

  if (!task) return <p>Loading...</p>;

  return (
    <div>
      <h1 style={{ marginBottom: '1rem' }}>{task.name}</h1>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.5rem' }}>
        <div style={{ background: '#fff', padding: '1rem', borderRadius: 8 }}>
          <h3 style={{ marginBottom: '0.5rem' }}>Task Info</h3>
          <p>
            <strong>Assignee:</strong> {task.assignee || 'Unassigned'}
          </p>
          <p>
            <strong>Priority:</strong> {task.priority}
          </p>
          <p>
            <strong>Created:</strong> {new Date(task.createTime).toLocaleString()}
          </p>
          {task.dueDate && (
            <p>
              <strong>Due:</strong> {new Date(task.dueDate).toLocaleString()}
            </p>
          )}
        </div>
        <div style={{ background: '#fff', padding: '1rem', borderRadius: 8 }}>
          <h3 style={{ marginBottom: '0.5rem' }}>Custom Fields</h3>
          <DynamicFieldForm
            processInstanceId={task.processInstanceId}
            processDefinitionKey={task.processDefinitionKey}
          />
        </div>
      </div>

      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem' }}>
        <button
          onClick={() => completeMutation.mutate()}
          style={{
            background: '#388e3c',
            color: '#fff',
            padding: '0.5rem 1rem',
            border: 'none',
            borderRadius: 4,
            cursor: 'pointer',
          }}
        >
          Complete Task
        </button>
      </div>

      <div style={{ background: '#fff', padding: '1rem', borderRadius: 8 }}>
        <h3 style={{ marginBottom: '0.5rem' }}>Comments</h3>
        {comments.map((c: any) => (
          <div key={c.id} style={{ borderBottom: '1px solid #eee', padding: '0.5rem 0' }}>
            <strong>{c.userId}</strong>{' '}
            <span style={{ color: '#999', fontSize: '0.85rem' }}>{new Date(c.createdAt).toLocaleString()}</span>
            <p>{c.content}</p>
          </div>
        ))}
        <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
          <input
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="Add comment..."
            style={{ flex: 1, padding: '0.4rem' }}
          />
          <button onClick={() => comment && addCommentMutation.mutate(comment)} style={{ padding: '0.4rem 1rem' }}>
            Add
          </button>
        </div>
      </div>
    </div>
  );
}
