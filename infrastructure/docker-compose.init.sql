-- =============================================================================
-- Workflow Platform — PostgreSQL Database Initialisation
-- =============================================================================
-- Run as the postgres superuser (POSTGRES_USER) on first container startup.
-- Mounted as /docker-entrypoint-initdb.d/01-init.sql inside the postgres
-- container so it is executed automatically by the official postgres image
-- entrypoint when the data directory is empty.
--
-- NOTE: The default database (POSTGRES_DB=postgres) is created by the image
-- entrypoint before this script runs, so it is intentionally omitted here.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Flowable BPM engine database
-- Used by: workflow-engine service (Spring Boot + Flowable)
-- ---------------------------------------------------------------------------
CREATE DATABASE flowable
    WITH ENCODING = 'UTF8'
         LC_COLLATE = 'en_US.utf8'
         LC_CTYPE = 'en_US.utf8'
         TEMPLATE = template0;

COMMENT ON DATABASE flowable IS 'Flowable BPM engine — process definitions, instances, tasks, history';

-- ---------------------------------------------------------------------------
-- Schema Service database
-- Used by: schema-service (form/schema definitions and field values)
-- ---------------------------------------------------------------------------
CREATE DATABASE schema_svc
    WITH ENCODING = 'UTF8'
         LC_COLLATE = 'en_US.utf8'
         LC_CTYPE = 'en_US.utf8'
         TEMPLATE = template0;

COMMENT ON DATABASE schema_svc IS 'Schema Service — form schemas, field definitions, and submitted field values';

-- ---------------------------------------------------------------------------
-- Notifications Service database
-- Used by: notifications-service (delivery log, preferences, templates)
-- ---------------------------------------------------------------------------
CREATE DATABASE notifications
    WITH ENCODING = 'UTF8'
         LC_COLLATE = 'en_US.utf8'
         LC_CTYPE = 'en_US.utf8'
         TEMPLATE = template0;

COMMENT ON DATABASE notifications IS 'Notifications Service — notification templates, delivery log, user preferences';

-- ---------------------------------------------------------------------------
-- Audit Service database
-- Used by: audit-service (immutable audit trail of all platform events)
-- ---------------------------------------------------------------------------
CREATE DATABASE audit_svc
    WITH ENCODING = 'UTF8'
         LC_COLLATE = 'en_US.utf8'
         LC_CTYPE = 'en_US.utf8'
         TEMPLATE = template0;

COMMENT ON DATABASE audit_svc IS 'Audit Service — immutable append-only audit trail for compliance';

-- ---------------------------------------------------------------------------
-- Scheduler Service database
-- Used by: scheduler-service (cron schedules, Quartz job store)
-- ---------------------------------------------------------------------------
CREATE DATABASE scheduler
    WITH ENCODING = 'UTF8'
         LC_COLLATE = 'en_US.utf8'
         LC_CTYPE = 'en_US.utf8'
         TEMPLATE = template0;

COMMENT ON DATABASE scheduler IS 'Scheduler Service — Quartz job store, cron schedule definitions';

-- ---------------------------------------------------------------------------
-- Kong API Gateway database
-- Used by: Kong Gateway (routes, plugins, consumers, credentials)
-- ---------------------------------------------------------------------------
CREATE DATABASE kong
    WITH ENCODING = 'UTF8'
         LC_COLLATE = 'en_US.utf8'
         LC_CTYPE = 'en_US.utf8'
         TEMPLATE = template0;

COMMENT ON DATABASE kong IS 'Kong API Gateway — routes, services, plugins, consumers';

-- ---------------------------------------------------------------------------
-- Keycloak Identity Provider database
-- Used by: Keycloak (realms, clients, users, roles, sessions)
-- ---------------------------------------------------------------------------
CREATE DATABASE keycloak
    WITH ENCODING = 'UTF8'
         LC_COLLATE = 'en_US.utf8'
         LC_CTYPE = 'en_US.utf8'
         TEMPLATE = template0;

COMMENT ON DATABASE keycloak IS 'Keycloak IAM — realms, clients, users, roles, sessions';

-- ---------------------------------------------------------------------------
-- Grant privileges
-- Application-specific roles/users should be created and granted here or
-- in subsequent migration scripts run by each service on startup (Flyway).
-- The superuser (POSTGRES_USER) has full access to all databases by default.
-- ---------------------------------------------------------------------------

-- Example: create dedicated app users (uncomment and customise as needed)
-- CREATE USER flowable_app   WITH PASSWORD 'changeme' NOSUPERUSER NOCREATEDB NOCREATEROLE;
-- CREATE USER schema_app     WITH PASSWORD 'changeme' NOSUPERUSER NOCREATEDB NOCREATEROLE;
-- CREATE USER notifications_app WITH PASSWORD 'changeme' NOSUPERUSER NOCREATEDB NOCREATEROLE;
-- CREATE USER audit_app      WITH PASSWORD 'changeme' NOSUPERUSER NOCREATEDB NOCREATEROLE;
-- CREATE USER scheduler_app  WITH PASSWORD 'changeme' NOSUPERUSER NOCREATEDB NOCREATEROLE;
-- CREATE USER kong_app       WITH PASSWORD 'changeme' NOSUPERUSER NOCREATEDB NOCREATEROLE;
-- CREATE USER keycloak_app   WITH PASSWORD 'changeme' NOSUPERUSER NOCREATEDB NOCREATEROLE;

-- GRANT ALL PRIVILEGES ON DATABASE flowable       TO flowable_app;
-- GRANT ALL PRIVILEGES ON DATABASE schema_svc     TO schema_app;
-- GRANT ALL PRIVILEGES ON DATABASE notifications  TO notifications_app;
-- GRANT ALL PRIVILEGES ON DATABASE audit_svc      TO audit_app;
-- GRANT ALL PRIVILEGES ON DATABASE scheduler      TO scheduler_app;
-- GRANT ALL PRIVILEGES ON DATABASE kong           TO kong_app;
-- GRANT ALL PRIVILEGES ON DATABASE keycloak       TO keycloak_app;
