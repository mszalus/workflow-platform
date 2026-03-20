import apiClient from "./apiClient";

export interface FieldValueEntry {
  fieldKey: string;
  fieldType: string;
  value: unknown;
}

export interface SaveFieldValuesRequest {
  processInstanceId: string;
  taskId?: string;
  values: FieldValueEntry[];
}

export interface FieldValueDto {
  id: string;
  taskId: string;
  processInstanceId: string;
  fieldKey: string;
  value: unknown;
  updatedAt: string;
  updatedBy: string;
}

export async function saveFieldValues(
  req: SaveFieldValuesRequest
): Promise<FieldValueDto[]> {
  const { data } = await apiClient.post<FieldValueDto[]>("/api/v1/field-values", req);
  return data;
}

export async function getFieldValues(
  processInstanceId: string,
  taskId?: string
): Promise<FieldValueDto[]> {
  const { data } = await apiClient.get<FieldValueDto[]>("/api/v1/field-values", {
    params: { processInstanceId, taskId },
  });
  return data;
}
