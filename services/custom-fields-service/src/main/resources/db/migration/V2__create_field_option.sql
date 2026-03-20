CREATE TABLE field_option (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    field_schema_id UUID NOT NULL REFERENCES field_schema(id) ON DELETE CASCADE,
    label VARCHAR(255) NOT NULL,
    value VARCHAR(255) NOT NULL,
    sort_order INT DEFAULT 0
);

CREATE INDEX idx_field_option_schema ON field_option(field_schema_id);
