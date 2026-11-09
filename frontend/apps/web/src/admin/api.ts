import { apiClient, type AuditEntry, type FieldSchema, type PagedResponse, type ProcessDefinition } from '../shared-ui';
import type { Project } from '../projects/ProjectContext';

export interface NewField {
  project: string;
  itemTypes: string[];
  fieldKey: string;
  label: string;
  fieldType: FieldSchema['fieldType'];
  required: boolean;
  options: { label: string; value: string; sortOrder: number }[];
}

export interface AuditQuery {
  userId?: string;
  eventType?: string;
  from?: string;
  to?: string;
  page: number;
  size: number;
}

export interface UpgradeSummary {
  workflowKey: string;
  openItems: number;
  sameStatus: number;
  walkedBack: number;
  reset: number;
  failed: number;
}

export function previewUpgrade(bpmnXml: string) {
  return apiClient
    .post<UpgradeSummary>('/workflow/workflows/upgrade-preview', { bpmnXml })
    .then((response) => response.data);
}

export function moveOpenItems(workflowKey: string) {
  return apiClient.post<UpgradeSummary>(`/workflow/workflows/${workflowKey}/upgrade`).then((response) => response.data);
}

export function listWorkflows() {
  return apiClient.get<ProcessDefinition[]>('/workflow/deployments').then((response) => response.data);
}

export function createProject(project: Project) {
  return apiClient.post<Project>('/projects', project).then((response) => response.data);
}

export function updateProject(key: string, changes: Pick<Project, 'name' | 'itemTypes'>) {
  return apiClient.put<Project>(`/projects/${key}`, changes).then((response) => response.data);
}

export function createField(field: NewField) {
  return apiClient.post<FieldSchema>('/fields/schemas', field).then((response) => response.data);
}

export function deleteField(id: string) {
  return apiClient.delete(`/fields/schemas/${id}`);
}

export function listAuditEntries(query: AuditQuery) {
  const params = new URLSearchParams();
  Object.entries(query).forEach(([name, value]) => {
    if (value !== undefined && value !== '') params.set(name, String(value));
  });
  return apiClient.get<PagedResponse<AuditEntry>>('/audit', { params }).then((response) => response.data);
}

export function downloadBpmn(name: string, xml: string) {
  const url = URL.createObjectURL(new Blob([xml], { type: 'application/xml' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = `${name || 'workflow'}.bpmn`;
  link.click();
  URL.revokeObjectURL(url);
}
