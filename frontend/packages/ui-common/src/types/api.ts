export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ApiError {
  status: number;
  code: string;
  message: string;
  timestamp: string;
  path?: string;
  details?: Record<string, string[]>;
}

export type WorkflowEventType =
  | "PROCESS_STARTED"
  | "PROCESS_COMPLETED"
  | "PROCESS_CANCELLED"
  | "TASK_CREATED"
  | "TASK_CLAIMED"
  | "TASK_UNCLAIMED"
  | "TASK_COMPLETED"
  | "TASK_DELEGATED"
  | "DEPLOYMENT_CREATED"
  | "DEPLOYMENT_DELETED";

export interface WorkflowEvent {
  eventId: string;
  eventType: WorkflowEventType;
  tenantId: string;
  timestamp: string;
  processDefinitionKey?: string;
  processInstanceId?: string;
  taskId?: string;
  deploymentId?: string;
  payload?: Record<string, unknown>;
}

export interface FieldDefinitionDto {
  id: string;
  fieldKey: string;
  label: string;
  fieldType: FieldType;
  required: boolean;
  displayOrder: number;
  options?: string[];
  validationRules?: Record<string, unknown>;
}

export type FieldType =
  | "TEXT"
  | "NUMBER"
  | "DATE"
  | "BOOLEAN"
  | "ENUM"
  | "FILE_REF"
  | "USER_REF";

export interface SchemaDto {
  id: string;
  processDefinitionKey: string;
  tenantId: string;
  version: number;
  fields: FieldDefinitionDto[];
  createdAt: string;
  updatedAt: string;
}
