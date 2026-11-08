import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { FieldSchema } from '@wfp/shared-ui';
import { createField, deleteField } from '../../admin/api';
import { apiErrorMessage, listFieldDefinitions } from '../../items/api';
import { useProjects, type Project } from '../../projects/ProjectContext';

const cell = { padding: '0.4rem 0.5rem', textAlign: 'left' as const, borderBottom: '1px solid #e0e0e0' };

const FIELD_TYPES: { value: FieldSchema['fieldType']; label: string }[] = [
  { value: 'TEXT', label: 'Text' },
  { value: 'TEXTAREA', label: 'Long text' },
  { value: 'NUMBER', label: 'Number' },
  { value: 'DATE', label: 'Date' },
  { value: 'DATETIME', label: 'Date and time' },
  { value: 'BOOLEAN', label: 'Yes or no' },
  { value: 'DROPDOWN', label: 'Select' },
];

function typeLabel(fieldType: FieldSchema['fieldType']) {
  return FIELD_TYPES.find((type) => type.value === fieldType)?.label ?? fieldType;
}

function NewFieldForm({ project, onDone }: { project: Project; onDone: () => void }) {
  const queryClient = useQueryClient();
  const [label, setLabel] = useState('');
  const [fieldKey, setFieldKey] = useState('');
  const [fieldType, setFieldType] = useState<FieldSchema['fieldType']>('TEXT');
  const [required, setRequired] = useState(false);
  const [itemTypes, setItemTypes] = useState<string[]>([]);
  const [optionLines, setOptionLines] = useState('');

  const create = useMutation({
    mutationFn: () =>
      createField({
        project: project.key,
        itemTypes,
        fieldKey,
        label,
        fieldType,
        required,
        options: optionLines
          .split('\n')
          .map((line) => line.trim())
          .filter((line) => line !== '')
          .map((line, sortOrder) => ({ label: line, value: line, sortOrder })),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['fieldDefinitions'] });
      onDone();
    },
  });
  const toggleItemType = (name: string) =>
    setItemTypes(itemTypes.includes(name) ? itemTypes.filter((other) => other !== name) : [...itemTypes, name]);
  const submit = (event: FormEvent) => {
    event.preventDefault();
    create.mutate();
  };

  return (
    <form
      onSubmit={submit}
      aria-label="New field"
      style={{ background: '#fff', padding: '1rem', borderRadius: 4, marginBottom: '1rem' }}
    >
      <label>
        Name <input required value={label} onChange={(event) => setLabel(event.target.value)} />
      </label>{' '}
      <label>
        Key <input required value={fieldKey} onChange={(event) => setFieldKey(event.target.value)} />
      </label>{' '}
      <label>
        Type{' '}
        <select value={fieldType} onChange={(event) => setFieldType(event.target.value as FieldSchema['fieldType'])}>
          {FIELD_TYPES.map((type) => (
            <option key={type.value} value={type.value}>
              {type.label}
            </option>
          ))}
        </select>
      </label>{' '}
      <label>
        <input type="checkbox" checked={required} onChange={(event) => setRequired(event.target.checked)} /> Required
      </label>
      <fieldset style={{ margin: '0.5rem 0' }}>
        <legend>Item types (none means all)</legend>
        {project.itemTypes.map((type) => (
          <label key={type.name} style={{ marginRight: '0.75rem' }}>
            <input type="checkbox" checked={itemTypes.includes(type.name)} onChange={() => toggleItemType(type.name)} />{' '}
            {type.name}
          </label>
        ))}
      </fieldset>
      {fieldType === 'DROPDOWN' && (
        <label style={{ display: 'block' }}>
          Options, one per line
          <textarea
            required
            value={optionLines}
            onChange={(event) => setOptionLines(event.target.value)}
            style={{ display: 'block', width: '100%' }}
          />
        </label>
      )}
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

function ProjectFields({ project }: { project: Project }) {
  const queryClient = useQueryClient();
  const [creating, setCreating] = useState(false);
  const { data: definitions = [] } = useQuery({
    queryKey: ['fieldDefinitions', project.key],
    queryFn: () => listFieldDefinitions(project.key),
  });
  const remove = useMutation({
    mutationFn: deleteField,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['fieldDefinitions'] }),
  });
  const confirmAndRemove = (definition: FieldSchema) => {
    if (window.confirm(`Delete the field ${definition.label}?`)) remove.mutate(definition.id);
  };

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
        <h1 style={{ fontSize: '1.4rem', margin: 0 }}>Fields · {project.key}</h1>
        <button onClick={() => setCreating(true)}>+ New field</button>
      </div>
      {creating && <NewFieldForm project={project} onDone={() => setCreating(false)} />}
      {remove.isError && <p role="alert">{apiErrorMessage(remove.error)}</p>}
      <table style={{ width: '100%', borderCollapse: 'collapse', background: '#fff' }}>
        <thead>
          <tr>
            {['Name', 'Key', 'Type', 'Item types', 'Required', ''].map((heading) => (
              <th key={heading} style={cell}>
                {heading}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {definitions.map((definition) => (
            <tr key={definition.id}>
              <td style={cell}>{definition.label}</td>
              <td style={cell}>{definition.fieldKey}</td>
              <td style={cell}>{typeLabel(definition.fieldType)}</td>
              <td style={cell}>{definition.itemTypes?.join(', ') || 'all'}</td>
              <td style={cell}>{definition.required ? 'yes' : 'no'}</td>
              <td style={cell}>
                <button aria-label={`Delete ${definition.label}`} onClick={() => confirmAndRemove(definition)}>
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {definitions.length === 0 && <p>This project has no custom fields.</p>}
    </>
  );
}

export default function FieldsPage() {
  const { currentProject } = useProjects();
  if (!currentProject) {
    return <p>Create a project first.</p>;
  }
  return <ProjectFields key={currentProject.key} project={currentProject} />;
}
