# Cloud SQL for PostgreSQL 16 — private IP, one shared instance, per-service schemas
# Uses the same schema-per-service layout as the local docker-compose stack.
# Connects via private IP (VPC peering, no proxy needed from GKE pods).

resource "random_id" "suffix" {
  byte_length = 4
}

resource "google_sql_database_instance" "postgres" {
  name             = "wfp-${var.environment}-${random_id.suffix.hex}"
  database_version = "POSTGRES_16"
  region           = var.region
  project          = var.project_id

  # Prevent accidental deletion in production
  deletion_protection = var.ha_enabled

  settings {
    tier              = var.tier
    availability_type = var.ha_enabled ? "REGIONAL" : "ZONAL"
    disk_size         = var.disk_gb
    disk_type         = "PD_SSD"
    disk_autoresize   = true

    ip_configuration {
      ipv4_enabled    = false  # Private IP only — no public endpoint
      private_network = var.network_id
    }

    backup_configuration {
      enabled                        = true
      start_time                     = "03:00"
      point_in_time_recovery_enabled = var.ha_enabled  # PITR requires HA
      backup_retention_settings {
        retained_backups = var.ha_enabled ? 14 : 7
      }
    }

    maintenance_window {
      day          = 7  # Sunday
      hour         = 4  # 04:00 UTC
      update_track = "stable"
    }

    database_flags {
      name  = "log_min_duration_statement"
      value = "200"  # Log queries slower than 200ms
    }

    database_flags {
      name  = "log_checkpoints"
      value = "on"
    }

    insights_config {
      query_insights_enabled  = true
      query_string_length     = 1024
      record_application_tags = true
      record_client_address   = false
    }
  }
}

# Application user (same credentials as docker-compose for env parity)
resource "google_sql_user" "wfp" {
  name     = "wfp"
  instance = google_sql_database_instance.postgres.name
  password = var.db_password
  project  = var.project_id
}

# One database; schemas (workflow, custom_fields, notification, audit, keycloak)
# are created at application startup via Flyway and the init-db.sql script.
resource "google_sql_database" "wfp" {
  name     = "wfp"
  instance = google_sql_database_instance.postgres.name
  project  = var.project_id
}
