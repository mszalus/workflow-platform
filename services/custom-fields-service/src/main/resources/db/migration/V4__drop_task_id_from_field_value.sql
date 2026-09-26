-- Custom field values are now process-instance scoped, not task-scoped.
-- Deduplicate: keep the most recently updated row per (field_schema_id, process_instance_id, tenant_id).
-- Use id as a tiebreaker so rows with equal updated_at are handled deterministically.
DELETE FROM field_value
WHERE id NOT IN (
    SELECT DISTINCT ON (field_schema_id, process_instance_id, tenant_id) id
    FROM field_value
    ORDER BY field_schema_id, process_instance_id, tenant_id, updated_at DESC NULLS LAST, id DESC
);

DROP INDEX IF EXISTS idx_field_value_unique;
ALTER TABLE field_value DROP COLUMN IF EXISTS task_id;
CREATE UNIQUE INDEX idx_field_value_unique ON field_value(field_schema_id, process_instance_id, tenant_id);
