import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { apiErrorMessage, createItem, listFieldDefinitions, type FieldValues, type Priority } from '../items/api';
import { PRIORITIES } from '../items/filters';
import { useProjects } from '../projects/ProjectContext';
import FieldInput from './FieldInput';

const fullWidth = { width: '100%' };

export default function CreateItemDialog({ onClose }: { onClose: () => void }) {
  const { projects, currentProject } = useProjects();
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const [projectKey, setProjectKey] = useState(currentProject?.key ?? '');
  const project = projects.find((candidate) => candidate.key === projectKey);
  const [typeName, setTypeName] = useState(project?.itemTypes[0]?.name ?? '');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [assignee, setAssignee] = useState('');
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [fields, setFields] = useState<FieldValues>({});

  const { data: definitions = [] } = useQuery({
    queryKey: ['fieldDefinitions', projectKey, typeName],
    queryFn: () => listFieldDefinitions(projectKey, typeName),
    enabled: projectKey !== '' && typeName !== '',
  });

  const create = useMutation({
    mutationFn: createItem,
    onSuccess: (item) => {
      queryClient.invalidateQueries({ queryKey: ['items'] });
      onClose();
      navigate(`/items/${item.key}`);
    },
  });

  const changeProject = (key: string) => {
    setProjectKey(key);
    setTypeName(projects.find((candidate) => candidate.key === key)?.itemTypes[0]?.name ?? '');
    setFields({});
  };
  const changeType = (name: string) => {
    setTypeName(name);
    setFields({});
  };
  const submit = (event: FormEvent) => {
    event.preventDefault();
    create.mutate({
      project: projectKey,
      type: typeName,
      title,
      description,
      assignee: assignee.trim() || undefined,
      priority,
      fields,
    });
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.4)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 20,
      }}
    >
      <form
        role="dialog"
        aria-label="Create item"
        onSubmit={submit}
        style={{
          background: '#fff',
          padding: '1.5rem',
          borderRadius: 4,
          width: 480,
          maxHeight: '90vh',
          overflow: 'auto',
        }}
      >
        <h2 style={{ marginTop: 0, fontSize: '1.2rem' }}>Create item</h2>
        {projects.length === 0 ? (
          <p>There are no projects yet. An admin needs to create one first.</p>
        ) : (
          <>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>
              <div style={{ fontSize: '0.85rem' }}>Project</div>
              <select value={projectKey} onChange={(event) => changeProject(event.target.value)} style={fullWidth}>
                {projects.map((candidate) => (
                  <option key={candidate.key} value={candidate.key}>
                    {candidate.key} - {candidate.name}
                  </option>
                ))}
              </select>
            </label>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>
              <div style={{ fontSize: '0.85rem' }}>Type</div>
              <select value={typeName} onChange={(event) => changeType(event.target.value)} style={fullWidth}>
                {project?.itemTypes.map((type) => (
                  <option key={type.name} value={type.name}>
                    {type.name}
                  </option>
                ))}
              </select>
            </label>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>
              <div style={{ fontSize: '0.85rem' }}>Title</div>
              <input required value={title} onChange={(event) => setTitle(event.target.value)} style={fullWidth} />
            </label>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>
              <div style={{ fontSize: '0.85rem' }}>Description</div>
              <textarea
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                style={fullWidth}
              />
            </label>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>
              <div style={{ fontSize: '0.85rem' }}>Assignee</div>
              <input
                placeholder="username, or leave empty"
                value={assignee}
                onChange={(event) => setAssignee(event.target.value)}
                style={fullWidth}
              />
            </label>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>
              <div style={{ fontSize: '0.85rem' }}>Priority</div>
              <select
                value={priority}
                onChange={(event) => setPriority(event.target.value as Priority)}
                style={fullWidth}
              >
                {PRIORITIES.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            {definitions.map((definition) => (
              <FieldInput
                key={definition.id}
                definition={definition}
                value={fields[definition.fieldKey]}
                onChange={(value) => setFields({ ...fields, [definition.fieldKey]: value })}
              />
            ))}
          </>
        )}
        {create.isError && <p role="alert">{apiErrorMessage(create.error)}</p>}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1rem' }}>
          <button type="button" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" disabled={projects.length === 0 || create.isPending}>
            Create
          </button>
        </div>
      </form>
    </div>
  );
}
