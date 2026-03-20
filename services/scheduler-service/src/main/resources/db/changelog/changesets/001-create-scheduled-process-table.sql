--liquibase formatted sql

--changeset scheduler-service:001-create-scheduled-process-table
CREATE TABLE IF NOT EXISTS scheduled_process (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id        VARCHAR(100) NOT NULL,
    name             VARCHAR(255) NOT NULL,
    description      VARCHAR(1000),
    process_def_key  VARCHAR(255) NOT NULL,
    cron_expression  VARCHAR(120) NOT NULL,
    variables        JSONB,
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    quartz_group     VARCHAR(100),
    quartz_job_name  VARCHAR(255),
    last_fired_at    TIMESTAMPTZ,
    next_fire_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_scheduled_process PRIMARY KEY (id),
    CONSTRAINT uq_scheduled_process_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX IF NOT EXISTS idx_scheduled_process_tenant
    ON scheduled_process (tenant_id, enabled);

CREATE INDEX IF NOT EXISTS idx_scheduled_process_key
    ON scheduled_process (process_def_key);

CREATE TABLE IF NOT EXISTS schedule_execution (
    id                   UUID         NOT NULL DEFAULT gen_random_uuid(),
    scheduled_process_id UUID         NOT NULL,
    fired_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    status               VARCHAR(50)  NOT NULL DEFAULT 'RUNNING',
    process_instance_id  VARCHAR(255),
    error_message        TEXT,

    CONSTRAINT pk_schedule_execution PRIMARY KEY (id),
    CONSTRAINT fk_schedule_execution_process
        FOREIGN KEY (scheduled_process_id)
        REFERENCES scheduled_process (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_schedule_execution_process
    ON schedule_execution (scheduled_process_id);

CREATE INDEX IF NOT EXISTS idx_schedule_execution_fired_at
    ON schedule_execution (fired_at DESC);

--rollback DROP TABLE IF EXISTS schedule_execution;
--rollback DROP TABLE IF EXISTS scheduled_process;
