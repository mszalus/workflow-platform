CREATE TABLE IF NOT EXISTS wf_process_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_definition_key VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(255),
    icon_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_process_metadata_tenant ON wf_process_metadata(tenant_id);
CREATE INDEX idx_process_metadata_key_tenant ON wf_process_metadata(process_definition_key, tenant_id);

CREATE TABLE IF NOT EXISTS wf_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_instance_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64),
    user_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_comments_process ON wf_comments(process_instance_id, tenant_id);
CREATE INDEX idx_comments_task ON wf_comments(task_id, tenant_id);

CREATE TABLE IF NOT EXISTS wf_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_instance_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64),
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT,
    storage_key VARCHAR(500) NOT NULL,
    uploaded_by VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_attachments_process ON wf_attachments(process_instance_id, tenant_id);
