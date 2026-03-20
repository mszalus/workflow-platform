import apiClient from "./apiClient";
import type { SchemaDto, FieldDefinitionDto } from "@workflow/ui-common";

export interface CreateSchemaRequest {
  processDefinitionKey: string;
  fields: Omit<FieldDefinitionDto, "id">[];
}

export interface UpdateSchemaRequest {
  fields: Omit<FieldDefinitionDto, "id">[];
}

export async function getSchemaByProcessKey(
  processDefinitionKey: string
): Promise<SchemaDto> {
  const { data } = await apiClient.get<SchemaDto>(
    `/api/v1/schemas/active/${processDefinitionKey}`
  );
  return data;
}

export async function getSchema(id: string): Promise<SchemaDto> {
  const { data } = await apiClient.get<SchemaDto>(`/api/v1/schemas/${id}`);
  return data;
}

export async function listSchemas(processDefKey?: string): Promise<SchemaDto[]> {
  const { data } = await apiClient.get<SchemaDto[]>("/api/v1/schemas", {
    params: processDefKey ? { processDefKey } : undefined,
  });
  return data;
}

export async function createSchema(request: CreateSchemaRequest): Promise<SchemaDto> {
  const { data } = await apiClient.post<SchemaDto>("/api/v1/schemas", request);
  return data;
}

export async function updateSchema(
  id: string,
  request: UpdateSchemaRequest
): Promise<SchemaDto> {
  const { data } = await apiClient.put<SchemaDto>(`/api/v1/schemas/${id}`, request);
  return data;
}

export async function deleteSchema(id: string): Promise<void> {
  await apiClient.delete(`/api/v1/schemas/${id}`);
}
