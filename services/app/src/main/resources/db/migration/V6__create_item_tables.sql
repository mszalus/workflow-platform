CREATE TABLE wf_project (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    project_key VARCHAR(10) NOT NULL,
    name VARCHAR(255) NOT NULL,
    next_item_number INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_project_key_unique ON wf_project(tenant_id, project_key);

CREATE TABLE wf_item_type (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES wf_project(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    workflow_key VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX idx_item_type_name_unique ON wf_item_type(project_id, name);

CREATE TABLE wf_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    item_key VARCHAR(30) NOT NULL,
    project_id UUID NOT NULL REFERENCES wf_project(id),
    item_type_id UUID NOT NULL REFERENCES wf_item_type(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    priority VARCHAR(20) NOT NULL,
    assignee VARCHAR(255),
    reporter VARCHAR(255) NOT NULL,
    status_key VARCHAR(255),
    status_name VARCHAR(255),
    status_category VARCHAR(20),
    workflow_version_id VARCHAR(255),
    run_id VARCHAR(64),
    fields JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_item_key_unique ON wf_item(tenant_id, item_key);
CREATE INDEX idx_item_project ON wf_item(tenant_id, project_id, updated_at DESC);
CREATE INDEX idx_item_assignee ON wf_item(tenant_id, assignee);

CREATE TABLE wf_item_transition (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    item_id UUID NOT NULL REFERENCES wf_item(id),
    from_status VARCHAR(255),
    to_status VARCHAR(255) NOT NULL,
    transition_id VARCHAR(255),
    actor VARCHAR(255) NOT NULL,
    workflow_version_id VARCHAR(255) NOT NULL,
    reason VARCHAR(255),
    transitioned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_item_transition_item ON wf_item_transition(item_id, transitioned_at);
