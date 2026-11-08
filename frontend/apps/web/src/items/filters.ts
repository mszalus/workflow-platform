import type { FieldSchema } from '../shared-ui';

export type Operator = 'eq' | 'ne' | 'in' | 'gte' | 'lte';

export interface FilterOption {
  label: string;
  value: string;
}

export interface FilterField {
  key: string;
  label: string;
  options?: FilterOption[];
  isDate?: boolean;
}

export interface FilterRow {
  id: number;
  field: string;
  operator: Operator;
  value: string;
}

export const OPERATORS: { value: Operator; label: string }[] = [
  { value: 'eq', label: 'is' },
  { value: 'ne', label: 'is not' },
  { value: 'in', label: 'is any of' },
  { value: 'gte', label: '>=' },
  { value: 'lte', label: '<=' },
];

export const STATUS_CATEGORIES: FilterOption[] = [
  { value: 'OPEN', label: 'Open' },
  { value: 'TODO', label: 'To do' },
  { value: 'IN_PROGRESS', label: 'In progress' },
  { value: 'DONE', label: 'Done' },
];

export const PRIORITIES: FilterOption[] = [
  { value: 'LOW', label: 'Low' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HIGH', label: 'High' },
];

const FIXED_FIELDS: FilterField[] = [
  { key: 'statusCategory', label: 'Status category', options: STATUS_CATEGORIES },
  { key: 'status', label: 'Status' },
  { key: 'type', label: 'Type' },
  { key: 'priority', label: 'Priority', options: PRIORITIES },
  { key: 'assignee', label: 'Assignee' },
  { key: 'reporter', label: 'Reporter' },
  { key: 'updatedAt', label: 'Updated', isDate: true },
];

const UNESCAPED_COMMA = /(?<!\\),/;

export function filterFields(definitions: FieldSchema[]): FilterField[] {
  const customFields = definitions.map((definition) => ({
    key: `cf.${definition.fieldKey}`,
    label: `${definition.label} (cf)`,
    options: definition.options?.length ? definition.options : undefined,
  }));
  return [...FIXED_FIELDS, ...customFields];
}

export function joinValues(values: string[]) {
  return values.map((value) => value.replaceAll(',', '\\,')).join(',');
}

export function splitValues(value: string) {
  return value.split(UNESCAPED_COMMA).map((part) => part.replaceAll('\\,', ','));
}

export function toFilterParams(rows: FilterRow[]) {
  return rows.filter((row) => row.value.trim() !== '').map((row) => `${row.field}:${row.operator}:${row.value}`);
}
