CREATE TABLE field_schema (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_definition_key VARCHAR(255) NOT NULL,
    field_key VARCHAR(255) NOT NULL,
    label VARCHAR(255) NOT NULL,
    field_type VARCHAR(50) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT DEFAULT 0,
    default_value TEXT,
    placeholder VARCHAR(255),
    validation_regex VARCHAR(500),
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_field_schema_tenant ON field_schema(tenant_id);
CREATE UNIQUE INDEX idx_field_schema_key_unique ON field_schema(process_definition_key, field_key, tenant_id);
