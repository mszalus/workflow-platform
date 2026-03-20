--liquibase formatted sql

--changeset notification-service:002-create-notification-preference-table
CREATE TABLE IF NOT EXISTS notification_preference (
    user_id         VARCHAR(255) NOT NULL,
    tenant_id       VARCHAR(255) NOT NULL,
    email_enabled   BOOLEAN      NOT NULL DEFAULT TRUE,
    in_app_enabled  BOOLEAN      NOT NULL DEFAULT TRUE,
    preferences     JSONB,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_notification_preference PRIMARY KEY (user_id)
);

CREATE INDEX IF NOT EXISTS idx_notif_pref_tenant
    ON notification_preference (tenant_id);

COMMENT ON TABLE notification_preference IS 'Per-user notification channel preferences';
COMMENT ON COLUMN notification_preference.preferences IS 'JSONB map of notification-type overrides';

--rollback DROP TABLE IF EXISTS notification_preference;
