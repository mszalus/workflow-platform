import type { FieldSchema } from '../shared-ui';
import { describe, expect, it } from 'vitest';
import { filterFields, joinValues, splitValues, toFilterParams } from './filters';

describe('filter values', () => {
  it('keeps a comma inside a value through join and split', () => {
    const values = ['Yes, please', 'No'];

    expect(joinValues(values)).toBe('Yes\\, please,No');
    expect(splitValues(joinValues(values))).toEqual(values);
  });
});

describe('toFilterParams', () => {
  it('writes field:operator:value and drops empty rows', () => {
    expect(
      toFilterParams([
        { id: 1, field: 'assignee', operator: 'eq', value: 'me' },
        { id: 2, field: 'priority', operator: 'eq', value: ' ' },
      ]),
    ).toEqual(['assignee:eq:me']);
  });
});

describe('filterFields', () => {
  it('adds custom fields after the fixed ones with the cf prefix', () => {
    const fields = filterFields([{ fieldKey: 'severity', label: 'Severity' } as FieldSchema]);

    expect(fields.at(-1)).toMatchObject({ key: 'cf.severity', label: 'Severity (cf)' });
    expect(fields[0].key).toBe('statusCategory');
  });
});
