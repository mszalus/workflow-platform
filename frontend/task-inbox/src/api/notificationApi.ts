import apiClient from "./apiClient";
import type { PagedResponse } from "@workflow/ui-common";

export type NotificationCategory =
  | "TASK_ASSIGNED"
  | "TASK_DUE_SOON"
  | "TASK_OVERDUE"
  | "PROCESS_COMPLETED"
  | "MENTION"
  | "SYSTEM";

export interface NotificationDto {
  id: string;
  userId: string;
  tenantId: string;
  category: NotificationCategory;
  title: string;
  message: string;
  read: boolean;
  taskId?: string;
  processInstanceId?: string;
  createdAt: string;
  readAt?: string;
}

export interface NotificationPreferencesDto {
  userId: string;
  emailEnabled: boolean;
  pushEnabled: boolean;
  categories: {
    [K in NotificationCategory]: boolean;
  };
}

export async function getNotifications(
  params: { page?: number; size?: number; unreadOnly?: boolean } = {}
): Promise<PagedResponse<NotificationDto>> {
  const { data } = await apiClient.get<PagedResponse<NotificationDto>>(
    "/api/v1/notifications",
    { params }
  );
  return data;
}

export async function markAsRead(id: string): Promise<NotificationDto> {
  const { data } = await apiClient.post<NotificationDto>(
    `/api/v1/notifications/${id}/read`
  );
  return data;
}

export async function markAllRead(): Promise<void> {
  await apiClient.post("/api/v1/notifications/read-all");
}

export async function getPreferences(): Promise<NotificationPreferencesDto> {
  const { data } = await apiClient.get<NotificationPreferencesDto>(
    "/api/v1/notifications/preferences"
  );
  return data;
}

export async function updatePreferences(
  preferences: Partial<NotificationPreferencesDto>
): Promise<NotificationPreferencesDto> {
  const { data } = await apiClient.put<NotificationPreferencesDto>(
    "/api/v1/notifications/preferences",
    preferences
  );
  return data;
}
