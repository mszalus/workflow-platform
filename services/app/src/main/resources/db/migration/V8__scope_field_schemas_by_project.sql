ALTER TABLE field_schema ALTER COLUMN process_definition_key DROP NOT NULL;
ALTER TABLE field_schema ADD COLUMN project_id UUID REFERENCES wf_project(id);

CREATE INDEX idx_field_schema_project ON field_schema(project_id, field_key);

CREATE TABLE field_schema_item_type (
    field_schema_id UUID NOT NULL REFERENCES field_schema(id) ON DELETE CASCADE,
    item_type_id UUID NOT NULL REFERENCES wf_item_type(id),
    PRIMARY KEY (field_schema_id, item_type_id)
);

CREATE TEMPORARY TABLE field_schema_project_copy ON COMMIT DROP AS
SELECT gen_random_uuid() AS copy_id, scoped.source_id, scoped.project_id
FROM (
    SELECT DISTINCT definition.id AS source_id, project.id AS project_id
    FROM field_schema definition
    JOIN wf_project project ON project.tenant_id = definition.tenant_id
    JOIN wf_item_type item_type ON item_type.project_id = project.id
        AND item_type.workflow_key = definition.process_definition_key
) scoped;

INSERT INTO field_schema (id, project_id, field_key, label, field_type, required, sort_order,
                          default_value, placeholder, validation_regex, tenant_id, created_at, updated_at)
SELECT copied.copy_id, copied.project_id, source.field_key, source.label, source.field_type, source.required,
       source.sort_order, source.default_value, source.placeholder, source.validation_regex,
       source.tenant_id, source.created_at, source.updated_at
FROM field_schema_project_copy copied
JOIN field_schema source ON source.id = copied.source_id;

INSERT INTO field_option (field_schema_id, label, value, sort_order)
SELECT copied.copy_id, source_option.label, source_option.value, source_option.sort_order
FROM field_schema_project_copy copied
JOIN field_option source_option ON source_option.field_schema_id = copied.source_id;

INSERT INTO field_schema_item_type (field_schema_id, item_type_id)
SELECT copied.copy_id, item_type.id
FROM field_schema_project_copy copied
JOIN field_schema source ON source.id = copied.source_id
JOIN wf_item_type item_type ON item_type.project_id = copied.project_id
    AND item_type.workflow_key = source.process_definition_key;
