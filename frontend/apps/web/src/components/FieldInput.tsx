import type { FieldSchema } from '../shared-ui';

interface FieldInputProps {
  definition: FieldSchema;
  value: unknown;
  onChange: (value: unknown) => void;
}

const HTML_INPUT_TYPES: Partial<Record<FieldSchema['fieldType'], string>> = {
  NUMBER: 'number',
  DATE: 'date',
  DATETIME: 'datetime-local',
};

export default function FieldInput({ definition, value, onChange }: FieldInputProps) {
  const label = definition.required ? `${definition.label} *` : definition.label;
  const text = value === undefined || value === null ? '' : String(value);

  if (definition.fieldType === 'BOOLEAN') {
    return (
      <label style={{ display: 'block' }}>
        <input type="checkbox" checked={value === true} onChange={(event) => onChange(event.target.checked)} /> {label}
      </label>
    );
  }
  return (
    <label style={{ display: 'block', marginBottom: '0.5rem' }}>
      <div style={{ fontSize: '0.85rem' }}>{label}</div>
      {definition.fieldType === 'DROPDOWN' ? (
        <select
          value={text}
          required={definition.required}
          onChange={(event) => onChange(event.target.value)}
          style={{ width: '100%' }}
        >
          <option value="" />
          {definition.options?.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      ) : definition.fieldType === 'TEXTAREA' ? (
        <textarea
          value={text}
          required={definition.required}
          onChange={(event) => onChange(event.target.value)}
          style={{ width: '100%' }}
        />
      ) : (
        <input
          type={HTML_INPUT_TYPES[definition.fieldType] ?? 'text'}
          value={text}
          required={definition.required}
          placeholder={definition.placeholder}
          onChange={(event) => onChange(event.target.value)}
          style={{ width: '100%' }}
        />
      )}
    </label>
  );
}
