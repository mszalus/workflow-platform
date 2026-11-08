import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createProject, listWorkflows } from '../../admin/api';
import { apiErrorMessage } from '../../items/api';
import { useProjects } from '../../projects/ProjectContext';

const cell = { padding: '0.4rem 0.5rem', textAlign: 'left' as const, borderBottom: '1px solid #e0e0e0' };

interface ItemTypeRow {
  id: number;
  name: string;
  workflowKey: string;
}

function NewProjectForm({ onDone }: { onDone: () => void }) {
  const queryClient = useQueryClient();
  const { data: workflows = [] } = useQuery({ queryKey: ['workflows'], queryFn: listWorkflows });
  const [key, setKey] = useState('');
  const [name, setName] = useState('');
  const [rows, setRows] = useState<ItemTypeRow[]>([{ id: 1, name: '', workflowKey: '' }]);
  const [nextId, setNextId] = useState(2);

  const create = useMutation({
    mutationFn: () =>
      createProject({
        key,
        name,
        itemTypes: rows.map((row) => ({ name: row.name, workflowKey: row.workflowKey })),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      onDone();
    },
  });

  const updateRow = (id: number, changes: Partial<ItemTypeRow>) =>
    setRows(rows.map((row) => (row.id === id ? { ...row, ...changes } : row)));
  const addRow = () => {
    setRows([...rows, { id: nextId, name: '', workflowKey: '' }]);
    setNextId(nextId + 1);
  };
  const submit = (event: FormEvent) => {
    event.preventDefault();
    create.mutate();
  };

  return (
    <form
      onSubmit={submit}
      aria-label="New project"
      style={{ background: '#fff', padding: '1rem', borderRadius: 4, marginBottom: '1rem' }}
    >
      <label>
        Key <input required value={key} onChange={(event) => setKey(event.target.value)} />
      </label>{' '}
      <label>
        Name <input required value={name} onChange={(event) => setName(event.target.value)} />
      </label>
      <h2 style={{ fontSize: '0.9rem', marginBottom: '0.25rem' }}>Item types</h2>
      {workflows.length === 0 && <p>Deploy a workflow first: every item type needs one.</p>}
      {rows.map((row) => (
        <div key={row.id} style={{ marginBottom: '0.25rem' }}>
          <input
            aria-label="Item type name"
            required
            value={row.name}
            onChange={(event) => updateRow(row.id, { name: event.target.value })}
          />{' '}
          uses workflow{' '}
          <select
            aria-label="Workflow"
            required
            value={row.workflowKey}
            onChange={(event) => updateRow(row.id, { workflowKey: event.target.value })}
          >
            <option value="" />
            {workflows.map((workflow) => (
              <option key={workflow.key} value={workflow.key}>
                {workflow.key}
              </option>
            ))}
          </select>{' '}
          {rows.length > 1 && (
            <button type="button" onClick={() => setRows(rows.filter((other) => other.id !== row.id))}>
              Remove
            </button>
          )}
        </div>
      ))}
      <button type="button" onClick={addRow}>
        + Add item type
      </button>
      {create.isError && <p role="alert">{apiErrorMessage(create.error)}</p>}
      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
        <button type="button" onClick={onDone}>
          Cancel
        </button>
        <button type="submit" disabled={create.isPending}>
          Save
        </button>
      </div>
    </form>
  );
}

export default function ProjectsPage() {
  const { projects } = useProjects();
  const [creating, setCreating] = useState(false);

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
        <h1 style={{ fontSize: '1.4rem', margin: 0 }}>Projects</h1>
        <button onClick={() => setCreating(true)}>+ New project</button>
      </div>
      {creating && <NewProjectForm onDone={() => setCreating(false)} />}
      <table style={{ width: '100%', borderCollapse: 'collapse', background: '#fff' }}>
        <thead>
          <tr>
            <th style={cell}>Key</th>
            <th style={cell}>Name</th>
            <th style={cell}>Item types -&gt; workflow</th>
          </tr>
        </thead>
        <tbody>
          {projects.map((project) => (
            <tr key={project.key}>
              <td style={cell}>{project.key}</td>
              <td style={cell}>{project.name}</td>
              <td style={cell}>{project.itemTypes.map((type) => `${type.name} -> ${type.workflowKey}`).join(', ')}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}
