import { describe, expect, it, vi } from 'vitest';
import { SelectEntry } from '@bpmn-io/properties-panel';
import { useService } from 'bpmn-js-properties-panel';
import { FlowablePropertiesProvider, STATUS_CATEGORY_OPTIONS } from './FlowablePropertiesProvider';

vi.mock('bpmn-js-properties-panel', () => ({ useService: vi.fn() }));
vi.mock('@bpmn-io/properties-panel', () => ({
  TextFieldEntry: vi.fn(),
  CheckboxEntry: vi.fn(),
  SelectEntry: vi.fn(),
  isTextFieldEntryEdited: vi.fn(),
  isCheckboxEntryEdited: vi.fn(),
  isSelectEntryEdited: vi.fn(),
}));

function element(types: string[], businessObject: Record<string, unknown> = {}) {
  return {
    businessObject: { $type: types[0], ...businessObject, $instanceOf: (type: string) => types.includes(type) },
  };
}

const statusSubprocess = { $type: 'bpmn:SubProcess', get: () => 'IN_PROGRESS' };

function groupIds(target: ReturnType<typeof element>) {
  const propertiesPanel = { registerProvider: vi.fn() };
  const provider = new (FlowablePropertiesProvider as any)(propertiesPanel);
  return provider
    .getGroups(target)([])
    .map((group: { id: string }) => group.id);
}

describe('FlowablePropertiesProvider', () => {
  it('registers with the properties panel', () => {
    const propertiesPanel = { registerProvider: vi.fn() };

    const provider = new (FlowablePropertiesProvider as any)(propertiesPanel);

    expect(propertiesPanel.registerProvider).toHaveBeenCalledWith(500, provider);
  });

  it('gives a user task the status, Flowable and async groups', () => {
    expect(groupIds(element(['bpmn:UserTask', 'bpmn:Activity']))).toEqual([
      'wfp-status',
      'flowable-user-task',
      'flowable-async',
    ]);
  });

  it('gives a subprocess a status category, but not an event subprocess', () => {
    expect(groupIds(element(['bpmn:SubProcess', 'bpmn:Activity']))).toEqual(['wfp-status', 'flowable-async']);
    expect(groupIds(element(['bpmn:SubProcess', 'bpmn:Activity'], { triggeredByEvent: true }))).toEqual([
      'flowable-async',
    ]);
  });

  it('offers no status category on steps inside a status subprocess, or on transactions', () => {
    expect(groupIds(element(['bpmn:UserTask', 'bpmn:Activity'], { $parent: statusSubprocess }))).toEqual([
      'flowable-user-task',
      'flowable-async',
    ]);
    expect(groupIds(element(['bpmn:Transaction', 'bpmn:SubProcess', 'bpmn:Activity']))).toEqual(['flowable-async']);
  });

  it('gives a service task its Flowable properties and no status', () => {
    expect(groupIds(element(['bpmn:ServiceTask', 'bpmn:Activity']))).toEqual([
      'flowable-service-task',
      'flowable-async',
    ]);
  });

  it('gives gateways and events only the async group, and flows nothing', () => {
    expect(groupIds(element(['bpmn:ExclusiveGateway', 'bpmn:Gateway']))).toEqual(['flowable-async']);
    expect(groupIds(element(['bpmn:StartEvent', 'bpmn:Event']))).toEqual(['flowable-async']);
    expect(groupIds(element(['bpmn:SequenceFlow']))).toEqual([]);
  });

  it('reads and writes the status category as wfp:statusCategory', () => {
    const modeling = { updateModdleProperties: vi.fn() };
    vi.mocked(useService).mockImplementation((name: string) =>
      name === 'modeling' ? modeling : (text: string) => text,
    );
    const userTask = element(['bpmn:UserTask', 'bpmn:Activity'], {
      get: (property: string) => (property === 'wfp:statusCategory' ? 'TODO' : undefined),
    });
    const provider = new (FlowablePropertiesProvider as any)({ registerProvider: vi.fn() });
    const statusEntry = provider.getGroups(userTask)([])[0].entries[0];

    statusEntry.component({ element: userTask, id: statusEntry.id });
    const select = vi.mocked(SelectEntry).mock.calls[0][0];

    expect(select.getValue()).toBe('TODO');
    select.setValue('DONE');
    select.setValue('');
    expect(modeling.updateModdleProperties).toHaveBeenNthCalledWith(1, userTask, userTask.businessObject, {
      'wfp:statusCategory': 'DONE',
    });
    expect(modeling.updateModdleProperties).toHaveBeenNthCalledWith(2, userTask, userTask.businessObject, {
      'wfp:statusCategory': undefined,
    });
  });

  it('offers the four status categories the backend accepts', () => {
    expect(STATUS_CATEGORY_OPTIONS.map((option) => option.value)).toEqual(['', 'OPEN', 'TODO', 'IN_PROGRESS', 'DONE']);
  });
});
