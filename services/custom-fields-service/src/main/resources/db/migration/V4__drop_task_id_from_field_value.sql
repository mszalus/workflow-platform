-- Custom field values are now process-instance scoped, not task-scoped.
-- Deduplicate: keep the most recently updated row per (field_schema_id, process_instance_id, tenant_id)
DELETE FROM field_value a
    USING field_value b
WHERE a.id <> b.id
  AND a.field_schema_id = b.field_schema_id
  AND a.process_instance_id = b.process_instance_id
  AND a.tenant_id = b.tenant_id
  AND a.updated_at < b.updated_at;

DROP INDEX IF EXISTS idx_field_value_unique;
ALTER TABLE field_value DROP COLUMN IF EXISTS task_id;
CREATE UNIQUE INDEX idx_field_value_unique ON field_value(field_schema_id, process_instance_id, tenant_id);
