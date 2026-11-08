import { apiClient, type FieldSchema, type PagedResponse } from '@wfp/shared-ui';

export type StatusCategory = 'OPEN' | 'TODO' | 'IN_PROGRESS' | 'DONE';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH';
export type FieldValues = Record<string, unknown>;

export interface Transition {
  id: string;
  name: string;
  targetStatusIds: string[];
}

export interface WorkflowStatus {
  id: string;
  name: string;
  category: StatusCategory;
}

export interface Step {
  id: string;
  name: string;
}

export interface Item {
  key: string;
  project: string;
  type: string;
  title: string;
  description?: string;
  priority: Priority;
  assignee?: string;
  reporter: string;
  statusId: string;
  statusName: string;
  statusCategory: StatusCategory;
  fields: FieldValues;
  workflowVersionId: string;
  createdAt: string;
  updatedAt: string;
  transitions: Transition[] | null;
  openSteps: Step[] | null;
  statuses: WorkflowStatus[] | null;
}

export interface ItemComment {
  id: string;
  userId: string;
  content: string;
  createdAt: string;
}

export interface ItemQuery {
  project?: string;
  filters?: string[];
  sort?: string;
  page?: number;
  size?: number;
}

export interface NewItem {
  project: string;
  type: string;
  title: string;
  description: string;
  assignee?: string;
  priority: Priority;
  fields: FieldValues;
}

export function listItems({ project, filters = [], sort, page = 0, size = 25 }: ItemQuery) {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (project) params.set('project', project);
  if (sort) params.set('sort', sort);
  filters.forEach((filter) => params.append('filter', filter));
  return apiClient.get<PagedResponse<Item>>('/items', { params }).then((response) => response.data);
}

export function getItem(key: string) {
  return apiClient.get<Item>(`/items/${key}`).then((response) => response.data);
}

export function createItem(item: NewItem) {
  return apiClient.post<Item>('/items', item).then((response) => response.data);
}

export function updateItem(
  key: string,
  changes: Partial<Pick<Item, 'description' | 'assignee' | 'priority' | 'fields'>>,
) {
  return apiClient.patch<Item>(`/items/${key}`, changes).then((response) => response.data);
}

export function transitionItem(key: string, transitionId: string) {
  return apiClient.post<Item>(`/items/${key}/transitions`, { transitionId }).then((response) => response.data);
}

export function completeStep(key: string, stepId: string) {
  return apiClient.post<Item>(`/items/${key}/steps`, { stepId }).then((response) => response.data);
}

export function listComments(key: string) {
  return apiClient.get<ItemComment[]>(`/items/${key}/comments`).then((response) => response.data);
}

export function addComment(key: string, content: string) {
  return apiClient.post<ItemComment>(`/items/${key}/comments`, { content }).then((response) => response.data);
}

export function listFieldDefinitions(project: string, type?: string) {
  const params = new URLSearchParams({ project });
  if (type) params.set('type', type);
  return apiClient.get<FieldSchema[]>('/fields/schemas', { params }).then((response) => response.data);
}

export function getWorkflowXml(versionId: string) {
  return apiClient
    .get<{ bpmnXml: string }>(`/workflow/deployments/${versionId}/bpmn`)
    .then((response) => response.data.bpmnXml);
}

export function apiErrorMessage(error: unknown) {
  const response = (error as { response?: { data?: { message?: string } } }).response;
  return response?.data?.message ?? 'Something went wrong. Please try again.';
}
