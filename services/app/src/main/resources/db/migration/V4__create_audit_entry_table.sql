CREATE TABLE audit_entry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255),
    tenant_id VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    details TEXT,
    source_service VARCHAR(100)
);

CREATE INDEX idx_audit_tenant_time ON audit_entry(tenant_id, timestamp DESC);
CREATE INDEX idx_audit_tenant_entity ON audit_entry(tenant_id, entity_type, entity_id);
CREATE INDEX idx_audit_tenant_user ON audit_entry(tenant_id, user_id);
