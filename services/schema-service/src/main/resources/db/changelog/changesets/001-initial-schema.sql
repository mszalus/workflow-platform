--liquibase formatted sql

--changeset schema-service:001-create-field-schema-table
CREATE TABLE IF NOT EXISTS field_schema (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(100) NOT NULL,
    process_def_key VARCHAR(255) NOT NULL,
    version         INT         NOT NULL DEFAULT 1,
    is_active       BOOLEAN     NOT NULL DEFAULT FALSE,
    description     VARCHAR(1000),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_field_schema PRIMARY KEY (id),
    CONSTRAINT uq_field_schema_tenant_key_version
        UNIQUE (tenant_id, process_def_key, version)
);

CREATE INDEX IF NOT EXISTS idx_field_schema_tenant_key_version
    ON field_schema (tenant_id, process_def_key, version);

CREATE INDEX IF NOT EXISTS idx_field_schema_tenant_active
    ON field_schema (tenant_id, is_active);

COMMENT ON TABLE field_schema IS 'Versioned field schema definitions per tenant and process definition key';
COMMENT ON COLUMN field_schema.is_active IS 'Only one version per (tenant_id, process_def_key) should be active at a time';

--rollback DROP TABLE IF EXISTS field_schema;

--changeset schema-service:002-create-field-definition-table
CREATE TABLE IF NOT EXISTS field_definition (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    schema_id        UUID        NOT NULL,
    field_key        VARCHAR(100) NOT NULL,
    label            VARCHAR(255) NOT NULL,
    field_type       VARCHAR(50)  NOT NULL,
    required         BOOLEAN      NOT NULL DEFAULT FALSE,
    validation_rules JSONB,
    options          JSONB,
    visible_on_tasks TEXT[],
    editable_by_roles TEXT[],
    display_order    INT          NOT NULL DEFAULT 0,
    placeholder      VARCHAR(500),
    help_text        VARCHAR(1000),

    CONSTRAINT pk_field_definition PRIMARY KEY (id),
    CONSTRAINT fk_field_def_schema
        FOREIGN KEY (schema_id) REFERENCES field_schema (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_field_def_schema_id
    ON field_definition (schema_id);

CREATE INDEX IF NOT EXISTS idx_field_def_key
    ON field_definition (field_key);

CREATE INDEX IF NOT EXISTS idx_field_def_schema_order
    ON field_definition (schema_id, display_order);

COMMENT ON TABLE field_definition IS 'Individual field definitions belonging to a field schema';
COMMENT ON COLUMN field_definition.validation_rules IS 'JSONB: arbitrary validation constraints (min, max, pattern, etc.)';
COMMENT ON COLUMN field_definition.options IS 'JSONB: array of {value, label} objects for ENUM type fields';
COMMENT ON COLUMN field_definition.visible_on_tasks IS 'Task definition keys on which this field is rendered';
COMMENT ON COLUMN field_definition.editable_by_roles IS 'Role names permitted to edit this field';

--rollback DROP TABLE IF EXISTS field_definition;

--changeset schema-service:003-create-field-value-table
CREATE TABLE IF NOT EXISTS field_value (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(100) NOT NULL,
    process_instance_id VARCHAR(255) NOT NULL,
    task_id             VARCHAR(255),
    field_key           VARCHAR(100) NOT NULL,
    field_type          VARCHAR(50)  NOT NULL,
    value_text          TEXT,
    value_number        NUMERIC(38, 10),
    value_date          DATE,
    value_boolean       BOOLEAN,
    value_json          JSONB,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_field_value PRIMARY KEY (id),
    CONSTRAINT uq_field_value_proc_task_key
        UNIQUE (process_instance_id, task_id, field_key)
);

CREATE INDEX IF NOT EXISTS idx_field_value_tenant_proc
    ON field_value (tenant_id, process_instance_id);

CREATE INDEX IF NOT EXISTS idx_field_value_tenant_task
    ON field_value (tenant_id, task_id)
    WHERE task_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_field_value_tenant_key
    ON field_value (tenant_id, field_key);

CREATE INDEX IF NOT EXISTS idx_field_value_proc_key
    ON field_value (process_instance_id, field_key);

-- GIN index for JSONB column searches
CREATE INDEX IF NOT EXISTS idx_field_value_json_gin
    ON field_value USING gin (value_json)
    WHERE value_json IS NOT NULL;

COMMENT ON TABLE field_value IS 'Stores per-instance/task typed field values';
COMMENT ON COLUMN field_value.value_json IS 'JSONB: used for FILE_REF, USER_REF, or any complex structured value';

--rollback DROP TABLE IF EXISTS field_value;

--changeset schema-service:004-update-timestamps-trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_field_schema_updated_at
    BEFORE UPDATE ON field_schema
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_field_value_updated_at
    BEFORE UPDATE ON field_value
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

--rollback DROP TRIGGER IF EXISTS trg_field_schema_updated_at ON field_schema;
--rollback DROP TRIGGER IF EXISTS trg_field_value_updated_at ON field_value;
--rollback DROP FUNCTION IF EXISTS update_updated_at_column();
