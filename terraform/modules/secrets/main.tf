# Secret Manager — stores sensitive config for GKE workloads.
# Secrets are created here with their initial values; rotate them via the GCP console
# or `gcloud secrets versions add` without re-running Terraform.
#
# The External Secrets Operator (ESO) installed in the cluster syncs these secrets
# into Kubernetes Secrets automatically. ESO needs to be installed separately:
#   helm install external-secrets external-secrets/external-secrets -n external-secrets --create-namespace
#
# Secret naming convention: wfp-<environment>-<purpose>

resource "google_secret_manager_secret" "db_password" {
  secret_id = "wfp-${var.environment}-db-password"
  project   = var.project_id

  replication {
    auto {}
  }

  labels = {
    environment = var.environment
    managed-by  = "terraform"
  }
}

resource "google_secret_manager_secret_version" "db_password" {
  secret      = google_secret_manager_secret.db_password.id
  secret_data = var.sql_db_password
}

# Keycloak admin password (used by Keycloak container + admin scripts)
resource "google_secret_manager_secret" "keycloak_admin_password" {
  secret_id = "wfp-${var.environment}-keycloak-admin-password"
  project   = var.project_id

  replication {
    auto {}
  }

  labels = {
    environment = var.environment
    managed-by  = "terraform"
  }
}

resource "google_secret_manager_secret_version" "keycloak_admin_password" {
  secret      = google_secret_manager_secret.keycloak_admin_password.id
  secret_data = var.keycloak_admin_password
}

# RabbitMQ password (shared across all services)
resource "google_secret_manager_secret" "rabbitmq_password" {
  secret_id = "wfp-${var.environment}-rabbitmq-password"
  project   = var.project_id

  replication {
    auto {}
  }

  labels = {
    environment = var.environment
    managed-by  = "terraform"
  }
}

resource "google_secret_manager_secret_version" "rabbitmq_password" {
  secret      = google_secret_manager_secret.rabbitmq_password.id
  secret_data = var.rabbitmq_password
}

# Grant the workload service account access to read all secrets
resource "google_secret_manager_secret_iam_member" "workload_db_password" {
  project   = var.project_id
  secret_id = google_secret_manager_secret.db_password.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.workload_sa_email}"
}

resource "google_secret_manager_secret_iam_member" "workload_keycloak_password" {
  project   = var.project_id
  secret_id = google_secret_manager_secret.keycloak_admin_password.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.workload_sa_email}"
}

resource "google_secret_manager_secret_iam_member" "workload_rabbitmq_password" {
  project   = var.project_id
  secret_id = google_secret_manager_secret.rabbitmq_password.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.workload_sa_email}"
}
