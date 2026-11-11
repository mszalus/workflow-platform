import { useState, type FormEvent } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import type { ProcessDefinition } from '../../shared-ui';
import { downloadBpmn, listWorkflows } from '../../admin/api';
import { getWorkflowXml } from '../../items/api';

const cell = { padding: '0.4rem 0.5rem', textAlign: 'left' as const, borderBottom: '1px solid #e0e0e0' };
const badge = { background: '#e3f2fd', padding: '0.1rem 0.4rem', borderRadius: 4, fontSize: '0.8rem' };

async function exportWorkflow(workflow: ProcessDefinition) {
  downloadBpmn(workflow.key, await getWorkflowXml(workflow.id));
}

export function renameProcess(xml: string, id: string, name: string) {
  const document = new DOMParser().parseFromString(xml, 'application/xml');
  const process = document.getElementsByTagNameNS('*', 'process')[0];
  const oldId = process.getAttribute('id');
  process.setAttribute('id', id);
  process.setAttribute('name', name);
  Array.from(document.getElementsByTagNameNS('*', 'BPMNPlane'))
    .filter((plane) => plane.getAttribute('bpmnElement') === oldId)
    .forEach((plane) => plane.setAttribute('bpmnElement', id));
  return new XMLSerializer().serializeToString(document);
}

function TemplateDialog({
  template,
  takenIds,
  onClose,
}: {
  template: ProcessDefinition;
  takenIds: string[];
  onClose: () => void;
}) {
  const navigate = useNavigate();
  const [id, setId] = useState('');
  const [name, setName] = useState('');
  const idTaken = takenIds.includes(id);

  const open = async (event: FormEvent) => {
    event.preventDefault();
    if (idTaken) {
      return;
    }
    const xml = renameProcess(await getWorkflowXml(template.id), id, name);
    navigate('/admin/workflows/new', { state: { xml, name } });
  };

  return (
    <form
      role="dialog"
      aria-label="Use as template"
      onSubmit={open}
      style={{ background: '#fff', padding: '1rem', borderRadius: 4, marginBottom: '1rem', maxWidth: 480 }}
    >
      <p style={{ marginTop: 0 }}>Copy {template.name} into a new workflow of your own.</p>
      <label style={{ display: 'block', marginBottom: '0.5rem' }}>
        New workflow id{' '}
        <input
          required
          pattern="[A-Za-z_][A-Za-z0-9_\-]*"
          title="Letters, digits, _ and -, starting with a letter"
          value={id}
          onChange={(event) => setId(event.target.value)}
        />
      </label>
      {idTaken && <p role="alert">A workflow with this id already exists.</p>}
      <label style={{ display: 'block', marginBottom: '0.5rem' }}>
        New workflow name <input required value={name} onChange={(event) => setName(event.target.value)} />
      </label>
      <button type="submit">Open in editor</button> <button onClick={onClose}>Cancel</button>
    </form>
  );
}

export default function WorkflowsPage() {
  const { data: workflows = [] } = useQuery({ queryKey: ['workflows'], queryFn: listWorkflows });
  const [template, setTemplate] = useState<ProcessDefinition>();

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
        <h1 style={{ fontSize: '1.4rem', margin: 0 }}>Workflows</h1>
        <Link to="/admin/workflows/new">+ New</Link>
      </div>
      {template && (
        <TemplateDialog
          template={template}
          takenIds={workflows.map((workflow) => workflow.key)}
          onClose={() => setTemplate(undefined)}
        />
      )}
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
              <td style={cell}>
                {workflow.name} {workflow.builtIn && <span style={badge}>Built-in</span>}
              </td>
              <td style={cell}>v{workflow.version}</td>
              <td style={cell}>
                {!workflow.builtIn && (
                  <>
                    <Link to={`/admin/workflows/${encodeURIComponent(workflow.id)}/edit`}>Edit</Link>{' '}
                  </>
                )}
                <button onClick={() => setTemplate(workflow)}>Use as template</button>{' '}
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
