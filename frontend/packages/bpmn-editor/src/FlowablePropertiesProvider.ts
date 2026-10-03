import {
  TextFieldEntry,
  isTextFieldEntryEdited,
  CheckboxEntry,
  isCheckboxEntryEdited,
  SelectEntry,
  isSelectEntryEdited,
} from '@bpmn-io/properties-panel';
import { useService } from 'bpmn-js-properties-panel';
import { is } from 'bpmn-js/lib/util/ModelUtil';

const LOW_PRIORITY = 500;

export const STATUS_CATEGORY_OPTIONS = [
  { value: '', label: '(not a status)' },
  { value: 'OPEN', label: 'Open' },
  { value: 'TODO', label: 'To do' },
  { value: 'IN_PROGRESS', label: 'In progress' },
  { value: 'DONE', label: 'Done' },
];

export function FlowablePropertiesProvider(this: any, propertiesPanel: any) {
  this.getGroups = function (element: any) {
    return function (groups: any[]) {
      if (canBeStatus(element)) {
        groups.push(statusGroup(element));
      }
      if (is(element, 'bpmn:UserTask')) {
        groups.push(userTaskGroup(element));
      }
      if (is(element, 'bpmn:ServiceTask')) {
        groups.push(serviceTaskGroup(element));
      }
      if (is(element, 'bpmn:Activity') || is(element, 'bpmn:Gateway') || is(element, 'bpmn:Event')) {
        groups.push(asyncGroup(element));
      }
      return groups;
    };
  };

  propertiesPanel.registerProvider(LOW_PRIORITY, this);
}

FlowablePropertiesProvider.$inject = ['propertiesPanel'];

// --- Status Group ---

function canBeStatus(element: any) {
  return is(element, 'bpmn:UserTask') || (is(element, 'bpmn:SubProcess') && !element.businessObject.triggeredByEvent);
}

function statusGroup(element: any) {
  return {
    id: 'wfp-status',
    label: 'Status',
    entries: [
      {
        id: 'wfp-statusCategory',
        component: StatusCategorySelect,
        isEdited: isSelectEntryEdited,
        element,
      },
    ],
  };
}

// --- User Task Group ---

function userTaskGroup(element: any) {
  return {
    id: 'flowable-user-task',
    label: 'Flowable',
    entries: [
      {
        id: 'flowable-assignee',
        component: FlowableTextField,
        isEdited: isTextFieldEntryEdited,
        label: 'Assignee',
        property: 'flowable:assignee',
        element,
      },
      {
        id: 'flowable-candidateUsers',
        component: FlowableTextField,
        isEdited: isTextFieldEntryEdited,
        label: 'Candidate Users',
        description: 'Comma-separated list of user IDs',
        property: 'flowable:candidateUsers',
        element,
      },
      {
        id: 'flowable-candidateGroups',
        component: FlowableTextField,
        isEdited: isTextFieldEntryEdited,
        label: 'Candidate Groups',
        description: 'Comma-separated list of group IDs',
        property: 'flowable:candidateGroups',
        element,
      },
      {
        id: 'flowable-formKey',
        component: FlowableTextField,
        isEdited: isTextFieldEntryEdited,
        label: 'Form Key',
        property: 'flowable:formKey',
        element,
      },
      {
        id: 'flowable-dueDate',
        component: FlowableTextField,
        isEdited: isTextFieldEntryEdited,
        label: 'Due Date',
        description: 'ISO date, duration (P3D), or expression (${dueDate})',
        property: 'flowable:dueDate',
        element,
      },
      {
        id: 'flowable-priority',
        component: FlowableTextField,
        isEdited: isTextFieldEntryEdited,
        label: 'Priority',
        property: 'flowable:priority',
        element,
      },
    ],
  };
}

// --- Service Task Group ---

function serviceTaskGroup(element: any) {
  return {
    id: 'flowable-service-task',
    label: 'Flowable',
    entries: [
      {
        id: 'flowable-class',
        component: FlowableTextField,
        isEdited: isTextFieldEntryEdited,
        label: 'Java Class',
        property: 'flowable:class',
        element,
      },
      {
        id: 'flowable-expression',
        component: FlowableTextField,
        isEdited: isTextFieldEntryEdited,
        label: 'Expression',
        property: 'flowable:expression',
        element,
      },
      {
        id: 'flowable-delegateExpression',
        component: FlowableTextField,
        isEdited: isTextFieldEntryEdited,
        label: 'Delegate Expression',
        property: 'flowable:delegateExpression',
        element,
      },
      {
        id: 'flowable-resultVariable',
        component: FlowableTextField,
        isEdited: isTextFieldEntryEdited,
        label: 'Result Variable',
        property: 'flowable:resultVariable',
        element,
      },
    ],
  };
}

// --- Async Group ---

function asyncGroup(element: any) {
  return {
    id: 'flowable-async',
    label: 'Asynchronous',
    entries: [
      {
        id: 'flowable-async',
        component: FlowableCheckbox,
        isEdited: isCheckboxEntryEdited,
        label: 'Async',
        property: 'flowable:async',
        element,
      },
      {
        id: 'flowable-asyncBefore',
        component: FlowableCheckbox,
        isEdited: isCheckboxEntryEdited,
        label: 'Async Before',
        property: 'flowable:asyncBefore',
        element,
      },
      {
        id: 'flowable-asyncAfter',
        component: FlowableCheckbox,
        isEdited: isCheckboxEntryEdited,
        label: 'Async After',
        property: 'flowable:asyncAfter',
        element,
      },
      {
        id: 'flowable-exclusive',
        component: FlowableCheckbox,
        isEdited: isCheckboxEntryEdited,
        label: 'Exclusive',
        property: 'flowable:exclusive',
        element,
      },
    ],
  };
}

// --- Entry Components ---

function StatusCategorySelect(props: any) {
  const { element, id } = props;
  const modeling = useService('modeling');
  const translate = useService('translate');

  const bo = element.businessObject;

  return SelectEntry({
    id,
    element,
    label: translate('Status category'),
    getValue: () => bo.get('wfp:statusCategory') || '',
    setValue: (value: string) =>
      modeling.updateModdleProperties(element, bo, { 'wfp:statusCategory': value || undefined }),
    getOptions: () => STATUS_CATEGORY_OPTIONS.map((option) => ({ ...option, label: translate(option.label) })),
  });
}

function FlowableTextField(props: any) {
  const { element, id, label, description, property } = props;
  const modeling = useService('modeling');
  const translate = useService('translate');
  const debounce = useService('debounceInput');

  const bo = element.businessObject;

  const getValue = () => bo.get(property) || '';
  const setValue = (value: string) => {
    modeling.updateModdleProperties(element, bo, { [property]: value || undefined });
  };

  return TextFieldEntry({
    id,
    element,
    label: translate(label),
    description: description ? translate(description) : undefined,
    getValue,
    setValue,
    debounce,
  });
}

function FlowableCheckbox(props: any) {
  const { element, id, label, property } = props;
  const modeling = useService('modeling');
  const translate = useService('translate');

  const bo = element.businessObject;

  const getValue = () => !!bo.get(property);
  const setValue = (value: boolean) => {
    modeling.updateModdleProperties(element, bo, { [property]: value });
  };

  return CheckboxEntry({
    id,
    element,
    label: translate(label),
    getValue,
    setValue,
  });
}

export default {
  __init__: ['flowablePropertiesProvider'],
  flowablePropertiesProvider: ['type', FlowablePropertiesProvider],
};
