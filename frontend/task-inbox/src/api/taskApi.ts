import apiClient from "./apiClient";
import type { PagedResponse } from "@workflow/ui-common";

export type TaskPriority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type TaskStatus = "CREATED" | "ASSIGNED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export interface TaskDto {
  id: string;
  name: string;
  description?: string;
  processDefinitionKey: string;
  processDefinitionName: string;
  processInstanceId: string;
  tenantId: string;
  assignee?: string;
  candidateUsers?: string[];
  candidateGroups?: string[];
  priority: TaskPriority;
  status: TaskStatus;
  dueDate?: string;
  followUpDate?: string;
  createdAt: string;
  completedAt?: string;
  variables?: Record<string, unknown>;
}

export interface CompleteTaskRequest {
  variables?: Record<string, unknown>;
  comment?: string;
}

export interface DelegateTaskRequest {
  userId: string;
  comment?: string;
}

export interface TaskFilter {
  status?: TaskStatus;
  assignee?: string;
  unassigned?: boolean;
  processDefinitionKey?: string;
  page?: number;
  size?: number;
}

export async function listMyTasks(
  params: TaskFilter = {}
): Promise<PagedResponse<TaskDto>> {
  const { data } = await apiClient.get<PagedResponse<TaskDto>>("/api/v1/tasks/my", {
    params,
  });
  return data;
}

export async function listUnassignedTasks(
  params: Omit<TaskFilter, "assignee" | "unassigned"> = {}
): Promise<PagedResponse<TaskDto>> {
  const { data } = await apiClient.get<PagedResponse<TaskDto>>("/api/v1/tasks/unassigned", {
    params,
  });
  return data;
}

export async function listCompletedTasks(
  params: Omit<TaskFilter, "status"> = {}
): Promise<PagedResponse<TaskDto>> {
  const { data } = await apiClient.get<PagedResponse<TaskDto>>("/api/v1/tasks/completed", {
    params,
  });
  return data;
}

export async function getTask(id: string): Promise<TaskDto> {
  const { data } = await apiClient.get<TaskDto>(`/api/v1/tasks/${id}`);
  return data;
}

export async function claimTask(id: string): Promise<TaskDto> {
  const { data } = await apiClient.post<TaskDto>(`/api/v1/tasks/${id}/claim`);
  return data;
}

export async function unclaimTask(id: string): Promise<TaskDto> {
  const { data } = await apiClient.post<TaskDto>(`/api/v1/tasks/${id}/unclaim`);
  return data;
}

export async function completeTask(
  id: string,
  request: CompleteTaskRequest = {}
): Promise<void> {
  await apiClient.post(`/api/v1/tasks/${id}/complete`, request);
}

export async function delegateTask(
  id: string,
  request: DelegateTaskRequest
): Promise<TaskDto> {
  const { data } = await apiClient.post<TaskDto>(`/api/v1/tasks/${id}/delegate`, request);
  return data;
}
