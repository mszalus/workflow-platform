CREATE TABLE wf_item_attachment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    item_id UUID NOT NULL REFERENCES wf_item(id),
    file_name VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    content BYTEA NOT NULL,
    uploaded_by VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_item_attachment_item ON wf_item_attachment(item_id, uploaded_at);
