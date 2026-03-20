import apiClient from "./apiClient";
import type { SchemaDto } from "@workflow/ui-common";

export async function getSchemaByProcessKey(
  processDefinitionKey: string
): Promise<SchemaDto> {
  const { data } = await apiClient.get<SchemaDto>(
    `/api/v1/schemas/active/${processDefinitionKey}`
  );
  return data;
}
