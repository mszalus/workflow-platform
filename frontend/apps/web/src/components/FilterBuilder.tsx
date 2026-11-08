import { useState } from 'react';
import { joinValues, OPERATORS, splitValues, type FilterField, type FilterRow, type Operator } from '../items/filters';

interface FilterBuilderProps {
  fields: FilterField[];
  onApply: (rows: FilterRow[]) => void;
}

function ValueInput({
  row,
  field,
  onChange,
}: {
  row: FilterRow;
  field: FilterField | undefined;
  onChange: (value: string) => void;
}) {
  if (field?.isDate) {
    return (
      <input
        aria-label="Value"
        type="date"
        value={row.value.slice(0, 10)}
        onChange={(event) => onChange(event.target.value ? `${event.target.value}T00:00:00Z` : '')}
      />
    );
  }
  if (field?.options && row.operator === 'in') {
    return (
      <select
        aria-label="Value"
        multiple
        value={row.value ? splitValues(row.value) : []}
        onChange={(event) => onChange(joinValues(Array.from(event.target.selectedOptions, (option) => option.value)))}
      >
        {field.options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    );
  }
  if (field?.options) {
    return (
      <select aria-label="Value" value={row.value} onChange={(event) => onChange(event.target.value)}>
        <option value="" />
        {field.options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    );
  }
  return (
    <input
      aria-label="Value"
      placeholder={row.operator === 'in' ? 'comma-separated values' : 'value'}
      value={row.value}
      onChange={(event) => onChange(event.target.value)}
    />
  );
}

export default function FilterBuilder({ fields, onApply }: FilterBuilderProps) {
  const [rows, setRows] = useState<FilterRow[]>([]);
  const [nextId, setNextId] = useState(1);

  const updateRow = (id: number, changes: Partial<FilterRow>) =>
    setRows(rows.map((row) => (row.id === id ? { ...row, ...changes } : row)));
  const addRow = () => {
    setRows([...rows, { id: nextId, field: fields[0].key, operator: 'eq', value: '' }]);
    setNextId(nextId + 1);
  };
  const clear = () => {
    setRows([]);
    onApply([]);
  };

  return (
    <div style={{ background: '#fff', padding: '0.75rem', borderRadius: 4, marginBottom: '1rem' }}>
      {rows.map((row) => (
        <div key={row.id} style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem' }}>
          <select
            aria-label="Field"
            value={row.field}
            onChange={(event) => updateRow(row.id, { field: event.target.value, value: '' })}
          >
            {fields.map((field) => (
              <option key={field.key} value={field.key}>
                {field.label}
              </option>
            ))}
          </select>
          <select
            aria-label="Operator"
            value={row.operator}
            onChange={(event) => updateRow(row.id, { operator: event.target.value as Operator, value: '' })}
          >
            {OPERATORS.map((operator) => (
              <option key={operator.value} value={operator.value}>
                {operator.label}
              </option>
            ))}
          </select>
          <ValueInput
            row={row}
            field={fields.find((field) => field.key === row.field)}
            onChange={(value) => updateRow(row.id, { value })}
          />
          <button aria-label="Remove filter" onClick={() => setRows(rows.filter((other) => other.id !== row.id))}>
            x
          </button>
        </div>
      ))}
      <div style={{ display: 'flex', gap: '0.5rem' }}>
        <button onClick={addRow}>+ Add filter</button>
        <span style={{ flex: 1 }} />
        <button onClick={clear}>Clear</button>
        <button onClick={() => onApply(rows)}>Apply</button>
      </div>
    </div>
  );
}
