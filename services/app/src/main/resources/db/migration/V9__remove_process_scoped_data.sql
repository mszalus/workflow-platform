DELETE FROM wf_comments WHERE item_id IS NULL;
ALTER TABLE wf_comments
    DROP COLUMN process_instance_id,
    DROP COLUMN task_id,
    ALTER COLUMN item_id SET NOT NULL;

DROP TABLE field_value;

DELETE FROM field_schema WHERE project_id IS NULL;
ALTER TABLE field_schema
    DROP COLUMN process_definition_key,
    ALTER COLUMN project_id SET NOT NULL;
