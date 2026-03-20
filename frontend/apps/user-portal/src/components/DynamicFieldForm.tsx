import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@wfp/shared-ui';
import type { FieldValue } from '@wfp/shared-ui';

interface Props {
  processInstanceId: string;
  taskId?: string;
}

export default function DynamicFieldForm({ processInstanceId, taskId }: Props) {
  const params = new URLSearchParams({ processInstanceId });
  if (taskId) params.set('taskId', taskId);

  const { data: values = [] } = useQuery({
    queryKey: ['field-values', processInstanceId, taskId],
    queryFn: () => apiClient.get(`/api/fields/values?${params}`).then((r) => r.data),
  });

  if (values.length === 0) return <p style={{ color: '#999' }}>No custom fields configured.</p>;

  return (
    <div>
      {values.map((v: FieldValue & { fieldKey: string; label: string; fieldType: string }) => (
        <div key={v.fieldSchemaId} style={{ marginBottom: '0.5rem' }}>
          <label style={{ display: 'block', fontWeight: 'bold', fontSize: '0.85rem', marginBottom: '0.2rem' }}>{v.label}</label>
          <span style={{ color: '#333' }}>{v.value || '--'}</span>
        </div>
      ))}
    </div>
  );
}
