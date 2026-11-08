import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import type { ProcessDefinition } from '@wfp/shared-ui';
import { downloadBpmn, listWorkflows } from '../../admin/api';
import { getWorkflowXml } from '../../items/api';

const cell = { padding: '0.4rem 0.5rem', textAlign: 'left' as const, borderBottom: '1px solid #e0e0e0' };

async function exportWorkflow(workflow: ProcessDefinition) {
  downloadBpmn(workflow.key, await getWorkflowXml(workflow.id));
}

export default function WorkflowsPage() {
  const { data: workflows = [] } = useQuery({ queryKey: ['workflows'], queryFn: listWorkflows });

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
        <h1 style={{ fontSize: '1.4rem', margin: 0 }}>Workflows</h1>
        <Link to="/admin/workflows/new">+ New</Link>
      </div>
      <table style={{ width: '100%', borderCollapse: 'collapse', background: '#fff' }}>
        <thead>
          <tr>
            <th style={cell}>Key</th>
            <th style={cell}>Name</th>
            <th style={cell}>Latest</th>
            <th style={cell}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {workflows.map((workflow) => (
            <tr key={workflow.id}>
              <td style={cell}>{workflow.key}</td>
              <td style={cell}>{workflow.name}</td>
              <td style={cell}>v{workflow.version}</td>
              <td style={cell}>
                <Link to={`/admin/workflows/${encodeURIComponent(workflow.id)}/edit`}>Edit</Link>{' '}
                <button onClick={() => exportWorkflow(workflow)}>Export BPMN</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {workflows.length === 0 && <p>No workflows deployed yet.</p>}
    </>
  );
}
