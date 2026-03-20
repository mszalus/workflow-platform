import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@wfp/shared-ui';
import type { FieldSchema } from '@wfp/shared-ui';

const FIELD_TYPES = ['TEXT', 'TEXTAREA', 'NUMBER', 'DATE', 'DATETIME', 'BOOLEAN', 'DROPDOWN', 'MULTI_SELECT', 'FILE', 'USER_PICKER'];

export default function CustomFieldEditor() {
  const [selectedProcess, setSelectedProcess] = useState('');
  const [newField, setNewField] = useState({ fieldKey: '', label: '', fieldType: 'TEXT', required: false });
  const queryClient = useQueryClient();

  const { data: processes = [] } = useQuery({
    queryKey: ['process-defs'],
    queryFn: () => apiClient.get('/api/workflow/deployments').then((r) => r.data),
  });

  const { data: schemas = [] } = useQuery({
    queryKey: ['field-schemas', selectedProcess],
    queryFn: () => apiClient.get(`/api/fields/schemas?processDefinitionKey=${selectedProcess}`).then((r) => r.data),
    enabled: !!selectedProcess,
  });

  const createMutation = useMutation({
    mutationFn: (data: any) => apiClient.post('/api/fields/schemas', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['field-schemas', selectedProcess] });
      setNewField({ fieldKey: '', label: '', fieldType: 'TEXT', required: false });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => apiClient.delete(`/api/fields/schemas/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['field-schemas', selectedProcess] }),
  });

  return (
    <div>
      <h1 style={{ marginBottom: '1rem' }}>Custom Field Editor</h1>
      <select value={selectedProcess} onChange={(e) => setSelectedProcess(e.target.value)}
        style={{ padding: '0.5rem', marginBottom: '1rem', minWidth: 250 }}>
        <option value="">Select a process definition...</option>
        {processes.map((p: any) => <option key={p.key} value={p.key}>{p.name}</option>)}
      </select>

      {selectedProcess && (
        <>
          <div style={{ background: '#fff', padding: '1rem', borderRadius: 8, marginBottom: '1rem' }}>
            <h3 style={{ marginBottom: '0.5rem' }}>Add Field</h3>
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
              <input placeholder="Field Key" value={newField.fieldKey} onChange={(e) => setNewField({ ...newField, fieldKey: e.target.value })} style={{ padding: '0.4rem' }} />
              <input placeholder="Label" value={newField.label} onChange={(e) => setNewField({ ...newField, label: e.target.value })} style={{ padding: '0.4rem' }} />
              <select value={newField.fieldType} onChange={(e) => setNewField({ ...newField, fieldType: e.target.value })} style={{ padding: '0.4rem' }}>
                {FIELD_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
              <label><input type="checkbox" checked={newField.required} onChange={(e) => setNewField({ ...newField, required: e.target.checked })} /> Required</label>
              <button onClick={() => createMutation.mutate({ ...newField, processDefinitionKey: selectedProcess, sortOrder: schemas.length })}
                style={{ background: '#1976d2', color: '#fff', padding: '0.4rem 1rem', border: 'none', borderRadius: 4, cursor: 'pointer' }}>
                Add
              </button>
            </div>
          </div>

          <table style={{ width: '100%', background: '#fff', borderCollapse: 'collapse', borderRadius: 8 }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #eee' }}>
                <th style={{ textAlign: 'left', padding: '0.75rem' }}>Key</th>
                <th style={{ textAlign: 'left', padding: '0.75rem' }}>Label</th>
                <th style={{ textAlign: 'left', padding: '0.75rem' }}>Type</th>
                <th style={{ textAlign: 'left', padding: '0.75rem' }}>Required</th>
                <th style={{ textAlign: 'left', padding: '0.75rem' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {schemas.map((s: FieldSchema) => (
                <tr key={s.id} style={{ borderBottom: '1px solid #eee' }}>
                  <td style={{ padding: '0.75rem' }}>{s.fieldKey}</td>
                  <td style={{ padding: '0.75rem' }}>{s.label}</td>
                  <td style={{ padding: '0.75rem' }}>{s.fieldType}</td>
                  <td style={{ padding: '0.75rem' }}>{s.required ? 'Yes' : 'No'}</td>
                  <td style={{ padding: '0.75rem' }}>
                    <button onClick={() => deleteMutation.mutate(s.id)} style={{ color: '#d32f2f', background: 'none', border: 'none', cursor: 'pointer' }}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </div>
  );
}
