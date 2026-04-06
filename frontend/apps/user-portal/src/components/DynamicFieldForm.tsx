import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@wfp/shared-ui';
import { useState, useEffect } from 'react';

interface FieldSchema {
  id: string;
  fieldKey: string;
  label: string;
  fieldType: string;
  required: boolean;
  defaultValue: string | null;
  placeholder: string | null;
  options: string[];
}

interface FieldValueDto {
  fieldSchemaId: string;
  fieldKey: string;
  label: string;
  fieldType: string;
  value: string;
}

interface Props {
  processInstanceId: string;
  processDefinitionKey: string;
}

export default function DynamicFieldForm({ processInstanceId, processDefinitionKey }: Props) {
  const queryClient = useQueryClient();
  const [formValues, setFormValues] = useState<Record<string, string>>({});
  const [editing, setEditing] = useState(false);

  const { data: schemas = [] } = useQuery<FieldSchema[]>({
    queryKey: ['field-schemas', processDefinitionKey],
    queryFn: () => apiClient.get(`/fields/schemas?processDefinitionKey=${processDefinitionKey}`).then((r) => r.data),
    enabled: !!processDefinitionKey,
  });

  const { data: savedValues = [] } = useQuery<FieldValueDto[]>({
    queryKey: ['field-values', processInstanceId],
    queryFn: () => apiClient.get(`/fields/values?processInstanceId=${processInstanceId}`).then((r) => r.data),
  });

  useEffect(() => {
    const vals: Record<string, string> = {};
    schemas.forEach((s) => {
      const saved = savedValues.find((v) => v.fieldSchemaId === s.id);
      vals[s.fieldKey] = saved?.value ?? s.defaultValue ?? '';
    });
    setFormValues(vals);
  }, [schemas, savedValues]);

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, string>) =>
      apiClient.post('/fields/values', { processInstanceId, processDefinitionKey, values }),
    onSuccess: () => {
      setEditing(false);
      queryClient.invalidateQueries({ queryKey: ['field-values', processInstanceId] });
    },
  });

  if (schemas.length === 0) return <p style={{ color: '#999' }}>No custom fields configured.</p>;

  return (
    <div>
      {schemas.map((s) => (
        <div key={s.id} style={{ marginBottom: '0.5rem' }}>
          <label style={{ display: 'block', fontWeight: 'bold', fontSize: '0.85rem', marginBottom: '0.2rem' }}>
            {s.label}{s.required && ' *'}
          </label>
          {editing ? (
            s.fieldType === 'SELECT' && s.options.length > 0 ? (
              <select
                value={formValues[s.fieldKey] ?? ''}
                onChange={(e) => setFormValues((prev) => ({ ...prev, [s.fieldKey]: e.target.value }))}
                style={{ width: '100%', padding: '0.3rem' }}
              >
                <option value="">-- Select --</option>
                {s.options.map((o) => <option key={o} value={o}>{o}</option>)}
              </select>
            ) : s.fieldType === 'BOOLEAN' ? (
              <select
                value={formValues[s.fieldKey] ?? ''}
                onChange={(e) => setFormValues((prev) => ({ ...prev, [s.fieldKey]: e.target.value }))}
                style={{ width: '100%', padding: '0.3rem' }}
              >
                <option value="">-- Select --</option>
                <option value="true">Yes</option>
                <option value="false">No</option>
              </select>
            ) : (
              <input
                type={s.fieldType === 'NUMBER' ? 'number' : s.fieldType === 'DATE' ? 'date' : 'text'}
                value={formValues[s.fieldKey] ?? ''}
                onChange={(e) => setFormValues((prev) => ({ ...prev, [s.fieldKey]: e.target.value }))}
                placeholder={s.placeholder ?? ''}
                style={{ width: '100%', padding: '0.3rem', boxSizing: 'border-box' }}
              />
            )
          ) : (
            <span style={{ color: '#333' }}>
              {savedValues.find((v) => v.fieldSchemaId === s.id)?.value || formValues[s.fieldKey] || '--'}
            </span>
          )}
        </div>
      ))}
      <div style={{ marginTop: '0.5rem' }}>
        {editing ? (
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button
              onClick={() => saveMutation.mutate(formValues)}
              disabled={saveMutation.isPending}
              style={{ background: '#1976d2', color: '#fff', padding: '0.3rem 0.8rem', border: 'none', borderRadius: 4, cursor: 'pointer' }}
            >
              {saveMutation.isPending ? 'Saving...' : 'Save'}
            </button>
            <button
              onClick={() => setEditing(false)}
              style={{ padding: '0.3rem 0.8rem', border: '1px solid #ccc', borderRadius: 4, cursor: 'pointer' }}
            >
              Cancel
            </button>
          </div>
        ) : (
          <button
            onClick={() => setEditing(true)}
            style={{ background: '#fff', color: '#333', padding: '0.3rem 0.8rem', border: '1px solid #ccc', borderRadius: 4, cursor: 'pointer' }}
          >
            Edit Fields
          </button>
        )}
      </div>
    </div>
  );
}
