import apiClient from "./apiClient";
import type { PagedResponse } from "@workflow/ui-common";

// ─── DTOs ────────────────────────────────────────────────────────────────────

export interface ProcessDefinitionDto {
  id: string;
  key: string;
  name: string;
  version: number;
  deploymentId: string;
  tenantId: string;
  resourceName: string;
  deployedAt: string;
}

export interface DeploymentDto {
  id: string;
  name: string;
  tenantId: string;
  deployedAt: string;
  processDefinitions: ProcessDefinitionDto[];
}

export interface ProcessInstanceDto {
  id: string;
  processDefinitionId: string;
  processDefinitionKey: string;
  processDefinitionName: string;
  businessKey?: string;
  tenantId: string;
  startedAt: string;
  endedAt?: string;
  state: "ACTIVE" | "SUSPENDED" | "COMPLETED" | "CANCELLED" | "INCIDENT";
  startedBy: string;
  variables?: Record<string, unknown>;
}

export interface StartProcessRequest {
  processDefinitionKey: string;
  businessKey?: string;
  variables?: Record<string, unknown>;
}

export interface DeployProcessRequest {
  name: string;
  bpmnXml: string;
}

// ─── Process Definitions ─────────────────────────────────────────────────────

export async function listProcessDefinitions(
  params: { page?: number; size?: number; tenantId?: string } = {}
): Promise<PagedResponse<ProcessDefinitionDto>> {
  const { data } = await apiClient.get<PagedResponse<ProcessDefinitionDto>>(
    "/api/v1/process-definitions",
    { params }
  );
  return data;
}

export async function getProcessDefinition(id: string): Promise<ProcessDefinitionDto> {
  const { data } = await apiClient.get<ProcessDefinitionDto>(
    `/api/v1/process-definitions/${id}`
  );
  return data;
}

export async function getProcessDefinitionXml(id: string): Promise<string> {
  const { data } = await apiClient.get<{ bpmnXml: string }>(
    `/api/v1/process-definitions/${id}/xml`
  );
  return data.bpmnXml;
}

// ─── Deployments ─────────────────────────────────────────────────────────────

export async function deployProcess(request: DeployProcessRequest): Promise<DeploymentDto> {
  const formData = new FormData();
  formData.append("name", request.name);
  formData.append(
    "file",
    new Blob([request.bpmnXml], { type: "application/xml" }),
    `${request.name}.bpmn`
  );

  const { data } = await apiClient.post<DeploymentDto>("/api/v1/deployments", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data;
}

export async function listDeployments(
  params: { page?: number; size?: number } = {}
): Promise<PagedResponse<DeploymentDto>> {
  const { data } = await apiClient.get<PagedResponse<DeploymentDto>>(
    "/api/v1/deployments",
    { params }
  );
  return data;
}

export async function deleteDeployment(id: string): Promise<void> {
  await apiClient.delete(`/api/v1/deployments/${id}`);
}

// ─── Process Instances ────────────────────────────────────────────────────────

export async function startProcessInstance(
  request: StartProcessRequest
): Promise<ProcessInstanceDto> {
  const { data } = await apiClient.post<ProcessInstanceDto>(
    "/api/v1/process-instances",
    request
  );
  return data;
}

export async function listProcessInstances(
  params: {
    processDefinitionKey?: string;
    state?: ProcessInstanceDto["state"];
    page?: number;
    size?: number;
  } = {}
): Promise<PagedResponse<ProcessInstanceDto>> {
  const { data } = await apiClient.get<PagedResponse<ProcessInstanceDto>>(
    "/api/v1/process-instances",
    { params }
  );
  return data;
}

export async function getProcessInstance(id: string): Promise<ProcessInstanceDto> {
  const { data } = await apiClient.get<ProcessInstanceDto>(
    `/api/v1/process-instances/${id}`
  );
  return data;
}

export async function cancelProcessInstance(id: string): Promise<void> {
  await apiClient.delete(`/api/v1/process-instances/${id}`);
}

export async function suspendProcessInstance(id: string): Promise<void> {
  await apiClient.post(`/api/v1/process-instances/${id}/suspend`);
}

export async function activateProcessInstance(id: string): Promise<void> {
  await apiClient.post(`/api/v1/process-instances/${id}/activate`);
}
