import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createProject, listWorkflows, updateProject } from '../../admin/api';
import { apiErrorMessage } from '../../items/api';
import { useProjects, type Project } from '../../projects/ProjectContext';

const cell = { padding: '0.4rem 0.5rem', textAlign: 'left' as const, borderBottom: '1px solid #e0e0e0' };

interface ItemTypeRow {
  id: number;
  name: string;
  workflowKey: string;
  saved: boolean;
}

function ProjectForm({ project, onDone }: { project?: Project; onDone: () => void }) {
  const queryClient = useQueryClient();
  const { data: workflows = [] } = useQuery({ queryKey: ['workflows'], queryFn: listWorkflows });
  const [key, setKey] = useState(project?.key ?? '');
  const [name, setName] = useState(project?.name ?? '');
  const [rows, setRows] = useState<ItemTypeRow[]>(
    project
      ? project.itemTypes.map((type, index) => ({ id: index, ...type, saved: true }))
      : [{ id: 0, name: '', workflowKey: '', saved: false }],
  );
  const [nextId, setNextId] = useState(rows.length);

  const save = useMutation({
    mutationFn: () => {
      const itemTypes = rows.map((row) => ({ name: row.name, workflowKey: row.workflowKey }));
      return project ? updateProject(project.key, { name, itemTypes }) : createProject({ key, name, itemTypes });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      onDone();
    },
  });

  const updateRow = (id: number, changes: Partial<ItemTypeRow>) =>
    setRows(rows.map((row) => (row.id === id ? { ...row, ...changes } : row)));
  const addRow = () => {
    setRows([...rows, { id: nextId, name: '', workflowKey: '', saved: false }]);
    setNextId(nextId + 1);
  };
  const submit = (event: FormEvent) => {
    event.preventDefault();
    save.mutate();
  };

  return (
    <form
      onSubmit={submit}
      aria-label={project ? `Edit project ${project.key}` : 'New project'}
      style={{ background: '#fff', padding: '1rem', borderRadius: 4, marginBottom: '1rem' }}
    >
      <label>
        Key <input required disabled={!!project} value={key} onChange={(event) => setKey(event.target.value)} />
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
            disabled={row.saved}
            value={row.name}
            onChange={(event) => updateRow(row.id, { name: event.target.value })}
          />{' '}
          uses workflow{' '}
          <select
            aria-label="Workflow"
            required
            disabled={row.saved}
            value={row.workflowKey}
            onChange={(event) => updateRow(row.id, { workflowKey: event.target.value })}
          >
            <option value="" />
            {row.saved && !workflows.some((workflow) => workflow.key === row.workflowKey) && (
              <option value={row.workflowKey}>{row.workflowKey}</option>
            )}
            {workflows.map((workflow) => (
              <option key={workflow.key} value={workflow.key}>
                {workflow.builtIn ? `${workflow.key} (built-in)` : workflow.key}
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
      {save.isError && <p role="alert">{apiErrorMessage(save.error)}</p>}
      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
        <button type="button" onClick={onDone}>
          Cancel
        </button>
        <button type="submit" disabled={save.isPending}>
          Save
        </button>
      </div>
    </form>
  );
}

export default function ProjectsPage() {
  const { projects } = useProjects();
  const [creating, setCreating] = useState(false);
  const [editingKey, setEditingKey] = useState<string>();
  const editedProject = projects.find((project) => project.key === editingKey);

  const startCreating = () => {
    setEditingKey(undefined);
    setCreating(true);
  };
  const startEditing = (key: string) => {
    setCreating(false);
    setEditingKey(key);
  };
  const closeForm = () => {
    setCreating(false);
    setEditingKey(undefined);
  };

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
        <h1 style={{ fontSize: '1.4rem', margin: 0 }}>Projects</h1>
        <button onClick={startCreating}>+ New project</button>
      </div>
      {creating && <ProjectForm onDone={closeForm} />}
      {editedProject && <ProjectForm key={editedProject.key} project={editedProject} onDone={closeForm} />}
      <table style={{ width: '100%', borderCollapse: 'collapse', background: '#fff' }}>
        <thead>
          <tr>
            <th style={cell}>Key</th>
            <th style={cell}>Name</th>
            <th style={cell}>Item types -&gt; workflow</th>
            <th style={cell} />
          </tr>
        </thead>
        <tbody>
          {projects.map((project) => (
            <tr key={project.key}>
              <td style={cell}>{project.key}</td>
              <td style={cell}>{project.name}</td>
              <td style={cell}>{project.itemTypes.map((type) => `${type.name} -> ${type.workflowKey}`).join(', ')}</td>
              <td style={cell}>
                <button aria-label={`Edit ${project.key}`} onClick={() => startEditing(project.key)}>
                  Edit
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}
