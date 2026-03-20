--liquibase formatted sql

--changeset audit-service:001-create-audit-event-table
-- NOTE: In production, convert this to a partitioned table using:
--   PARTITION BY RANGE (occurred_at)
-- and manage monthly partitions via pg_partman or a scheduled job.
CREATE TABLE IF NOT EXISTS audit_event (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    event_id     VARCHAR(255) NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    tenant_id    VARCHAR(100),
    actor_id     VARCHAR(255),
    occurred_at  TIMESTAMPTZ  NOT NULL,
    received_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    payload      JSONB        NOT NULL,
    topic        VARCHAR(255) NOT NULL,

    CONSTRAINT pk_audit_event PRIMARY KEY (id),
    CONSTRAINT uq_audit_event_id UNIQUE (event_id)
);

-- Index for tenant + time range queries (most common query pattern)
CREATE INDEX IF NOT EXISTS idx_audit_tenant_occurred
    ON audit_event (tenant_id, occurred_at DESC);

-- Index for event type filtering
CREATE INDEX IF NOT EXISTS idx_audit_event_type
    ON audit_event (event_type, occurred_at DESC);

-- Index for actor/user filtering
CREATE INDEX IF NOT EXISTS idx_audit_actor
    ON audit_event (actor_id, occurred_at DESC)
    WHERE actor_id IS NOT NULL;

-- GIN index for JSONB payload queries
CREATE INDEX IF NOT EXISTS idx_audit_payload_gin
    ON audit_event USING gin (payload);

COMMENT ON TABLE audit_event IS 'Immutable audit log; in production partition by occurred_at using pg_partman';
COMMENT ON COLUMN audit_event.event_id IS 'Source system event ID; unique constraint ensures idempotent consumption';
COMMENT ON COLUMN audit_event.payload IS 'Full JSONB event payload for ad-hoc queries';

--rollback DROP TABLE IF EXISTS audit_event;
