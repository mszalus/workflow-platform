CREATE TABLE field_value (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    field_schema_id UUID NOT NULL REFERENCES field_schema(id),
    process_instance_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64),
    value TEXT,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_field_value_process ON field_value(process_instance_id, tenant_id);
CREATE UNIQUE INDEX idx_field_value_unique ON field_value(field_schema_id, process_instance_id, COALESCE(task_id, ''), tenant_id);
