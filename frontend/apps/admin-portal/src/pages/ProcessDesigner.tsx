import { useState, useRef, useEffect } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { apiClient } from '@wfp/shared-ui';
import { BpmnEditor } from '@wfp/bpmn-editor';
import { useNavigate, useParams } from 'react-router-dom';

export default function ProcessDesigner() {
  const { id } = useParams<{ id: string }>();
  const [xml, setXml] = useState<string | undefined>();
  const [name, setName] = useState('');
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Load existing BPMN XML when editing a process definition
  const { data: bpmnData } = useQuery({
    queryKey: ['bpmn-xml', id],
    queryFn: () => apiClient.get(`/workflow/deployments/${id}/bpmn`).then((r) => r.data),
    enabled: !!id,
  });

  useEffect(() => {
    if (bpmnData?.bpmnXml) {
      setXml(bpmnData.bpmnXml);
    }
  }, [bpmnData]);

  const deployMutation = useMutation({
    mutationFn: (data: { name: string; bpmnXml: string }) => apiClient.post('/workflow/deployments', data),
    onSuccess: () => navigate('/processes'),
  });

  const handleImport = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (evt) => {
      const content = evt.target?.result as string;
      setXml(content);
      if (!name) {
        setName(file.name.replace(/\.(bpmn|xml|bpmn20\.xml)$/i, ''));
      }
    };
    reader.readAsText(file);
    // Reset input so the same file can be re-imported
    e.target.value = '';
  };

  const handleExport = () => {
    if (!xml) return;
    const blob = new Blob([xml], { type: 'application/xml' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = (name || 'process') + '.bpmn';
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 3rem)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <h1>Process Designer</h1>
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          <input
            ref={fileInputRef}
            type="file"
            accept=".bpmn,.xml,.bpmn20.xml"
            onChange={handleImport}
            style={{ display: 'none' }}
          />
          <button
            onClick={() => fileInputRef.current?.click()}
            style={{
              background: '#fff',
              color: '#333',
              padding: '0.5rem 1rem',
              border: '1px solid #ccc',
              borderRadius: 4,
              cursor: 'pointer',
            }}
          >
            Import
          </button>
          <button
            onClick={handleExport}
            disabled={!xml}
            style={{
              background: '#fff',
              color: '#333',
              padding: '0.5rem 1rem',
              border: '1px solid #ccc',
              borderRadius: 4,
              cursor: 'pointer',
            }}
          >
            Export
          </button>
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Process name"
            style={{ padding: '0.5rem', border: '1px solid #ccc', borderRadius: 4 }}
          />
          <button
            onClick={() => xml && name && deployMutation.mutate({ name, bpmnXml: xml })}
            disabled={!xml || !name || deployMutation.isPending}
            style={{
              background: '#1976d2',
              color: '#fff',
              padding: '0.5rem 1rem',
              border: 'none',
              borderRadius: 4,
              cursor: 'pointer',
            }}
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
