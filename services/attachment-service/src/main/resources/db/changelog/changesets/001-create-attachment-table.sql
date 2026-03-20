--liquibase formatted sql

--changeset attachment-service:001-create-attachment-table
CREATE TABLE IF NOT EXISTS attachment (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(100) NOT NULL,
    process_instance_id VARCHAR(255) NOT NULL,
    task_id             VARCHAR(255),
    field_key           VARCHAR(100),
    file_name           VARCHAR(500) NOT NULL,
    content_type        VARCHAR(255),
    file_size           BIGINT,
    object_key          VARCHAR(1000) NOT NULL,
    uploaded_by         VARCHAR(255) NOT NULL,
    uploaded_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    confirmed           BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,

    CONSTRAINT pk_attachment PRIMARY KEY (id),
    CONSTRAINT uq_attachment_object_key UNIQUE (object_key)
);

CREATE INDEX IF NOT EXISTS idx_attachment_tenant_proc
    ON attachment (tenant_id, process_instance_id)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_attachment_tenant_task
    ON attachment (tenant_id, task_id)
    WHERE task_id IS NOT NULL AND deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_attachment_field_key
    ON attachment (process_instance_id, field_key)
    WHERE field_key IS NOT NULL AND deleted = FALSE;

COMMENT ON TABLE attachment IS 'Metadata registry for files stored in MinIO';
COMMENT ON COLUMN attachment.object_key IS 'S3 object key: {tenantId}/{processInstanceId}/{fieldKey}/{filename}';
COMMENT ON COLUMN attachment.deleted IS 'Soft delete flag; object is also removed from MinIO on delete';

--rollback DROP TABLE IF EXISTS attachment;
