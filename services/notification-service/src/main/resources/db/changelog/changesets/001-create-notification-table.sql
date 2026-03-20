--liquibase formatted sql

--changeset notification-service:001-create-notification-table
CREATE TABLE IF NOT EXISTS notification (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(100) NOT NULL,
    recipient_id        VARCHAR(255) NOT NULL,
    recipient_email     VARCHAR(255),
    notification_type   VARCHAR(50)  NOT NULL,
    title               VARCHAR(500) NOT NULL,
    body                TEXT,
    reference_id        VARCHAR(255),
    reference_type      VARCHAR(100),
    action_url          VARCHAR(1000),
    is_read             BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at             TIMESTAMPTZ,
    email_sent          BOOLEAN      NOT NULL DEFAULT FALSE,
    email_sent_at       TIMESTAMPTZ,
    source_event_id     VARCHAR(255),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_notification PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_notification_recipient
    ON notification (recipient_id, is_read, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notification_tenant
    ON notification (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notification_ref
    ON notification (reference_id, reference_type)
    WHERE reference_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_notification_source_event
    ON notification (source_event_id)
    WHERE source_event_id IS NOT NULL;

COMMENT ON TABLE notification IS 'Persisted notifications for in-app and email delivery';
COMMENT ON COLUMN notification.source_event_id IS 'Kafka event ID for idempotency deduplication';

--rollback DROP TABLE IF EXISTS notification;
