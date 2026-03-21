import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { apiClient } from '@wfp/shared-ui';
import { BpmnEditor } from '@wfp/bpmn-editor';
import { useNavigate } from 'react-router-dom';

export default function ProcessDesigner() {
  const [xml, setXml] = useState<string | undefined>();
  const [name, setName] = useState('');
  const navigate = useNavigate();

  const deployMutation = useMutation({
    mutationFn: (data: { name: string; bpmnXml: string }) =>
      apiClient.post('/workflow/deployments', data),
    onSuccess: () => navigate('/processes'),
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 3rem)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <h1>Process Designer</h1>
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Process name"
            style={{ padding: '0.5rem', border: '1px solid #ccc', borderRadius: 4 }}
          />
          <button
            onClick={() => xml && name && deployMutation.mutate({ name, bpmnXml: xml })}
            disabled={!xml || !name || deployMutation.isPending}
            style={{ background: '#1976d2', color: '#fff', padding: '0.5rem 1rem', border: 'none', borderRadius: 4, cursor: 'pointer' }}
          >
            {deployMutation.isPending ? 'Deploying...' : 'Deploy'}
          </button>
        </div>
      </div>
      <div style={{ flex: 1 }}>
        <BpmnEditor xml={xml} onXmlChange={setXml} />
      </div>
    </div>
  );
}
