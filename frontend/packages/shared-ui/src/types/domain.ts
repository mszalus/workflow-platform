export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  tenantId: string;
  roles: string[];
}

export interface ProcessDefinition {
  id: string;
  key: string;
  name: string;
  version: number;
  category?: string;
  description?: string;
  deploymentId: string;
  tenantId: string;
  suspended: boolean;
}

export interface ProcessInstance {
  id: string;
  processDefinitionId: string;
  processDefinitionKey: string;
  processDefinitionName: string;
  businessKey?: string;
  startTime: string;
  endTime?: string;
  startUserId: string;
  tenantId: string;
  status: 'RUNNING' | 'COMPLETED' | 'CANCELLED' | 'SUSPENDED';
  variables?: Record<string, unknown>;
}

export interface Task {
  id: string;
  name: string;
  description?: string;
  assignee?: string;
  owner?: string;
  processInstanceId: string;
  processDefinitionId: string;
  processDefinitionKey: string;
  taskDefinitionKey: string;
  createTime: string;
  dueDate?: string;
  priority: number;
  tenantId: string;
  formKey?: string;
  candidateGroups?: string[];
  candidateUsers?: string[];
}

export interface Notification {
  id: string;
  userId: string;
  title: string;
  message: string;
  type: 'TASK_ASSIGNED' | 'TASK_COMPLETED' | 'PROCESS_COMPLETED' | 'SLA_BREACH' | 'INFO';
  read: boolean;
  createdAt: string;
  referenceId?: string;
  referenceType?: string;
}

export interface AuditEntry {
  id: string;
  eventType: string;
  entityType: string;
  entityId: string;
  userId: string;
  tenantId: string;
  timestamp: string;
  details: Record<string, unknown>;
}

export interface FieldSchema {
  id: string;
  processDefinitionKey: string;
  fieldKey: string;
  label: string;
  fieldType: FieldType;
  required: boolean;
  sortOrder: number;
  options?: FieldOption[];
  defaultValue?: string;
  placeholder?: string;
  validationRegex?: string;
}

export type FieldType =
  | 'TEXT'
  | 'TEXTAREA'
  | 'NUMBER'
  | 'DATE'
  | 'DATETIME'
  | 'BOOLEAN'
  | 'DROPDOWN'
  | 'MULTI_SELECT'
  | 'FILE'
  | 'USER_PICKER';

export interface FieldOption {
  id: string;
  label: string;
  value: string;
  sortOrder: number;
}

export interface FieldValue {
  id: string;
  fieldSchemaId: string;
  processInstanceId: string;
  value: string;
}
