# IAM — Workload Identity service accounts for GKE pods.
# Each service gets its own KSA (Kubernetes Service Account) + GSA (Google Service Account).
# Workload Identity binds KSA → GSA so pods can call GCP APIs without key files.

# Workload service account used by all application pods (shared, least-privilege baseline)
resource "google_service_account" "workload" {
  account_id   = "wfp-${var.environment}-workload"
  display_name = "WFP ${var.environment} workload SA (GKE pods)"
  project      = var.project_id
}

# Allow all pods in the wfp namespace to impersonate this service account
resource "google_service_account_iam_member" "workload_wi" {
  service_account_id = google_service_account.workload.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "serviceAccount:${var.project_id}.svc.id.goog[${var.k8s_namespace}/wfp-workload]"
}

# CI/CD service account for pushing images to Artifact Registry from GitHub Actions
resource "google_service_account" "cicd" {
  account_id   = "wfp-${var.environment}-cicd"
  display_name = "WFP ${var.environment} CI/CD SA (image push)"
  project      = var.project_id
}

# CI/CD SA can push images to Artifact Registry
resource "google_project_iam_member" "cicd_ar_writer" {
  project = var.project_id
  role    = "roles/artifactregistry.writer"
  member  = "serviceAccount:${google_service_account.cicd.email}"
}

# CI/CD SA can deploy to GKE (for kubectl/helm in CI)
resource "google_project_iam_member" "cicd_gke_developer" {
  project = var.project_id
  role    = "roles/container.developer"
  member  = "serviceAccount:${google_service_account.cicd.email}"
}

# Workload SA can read secrets from Secret Manager (for ExternalSecrets operator)
resource "google_project_iam_member" "workload_secret_accessor" {
  project = var.project_id
  role    = "roles/secretmanager.secretAccessor"
  member  = "serviceAccount:${google_service_account.workload.email}"
}

# Workload SA can write traces to Cloud Trace
resource "google_project_iam_member" "workload_trace_agent" {
  project = var.project_id
  role    = "roles/cloudtrace.agent"
  member  = "serviceAccount:${google_service_account.workload.email}"
}

# Workload SA can write metrics to Cloud Monitoring / GMP
resource "google_project_iam_member" "workload_monitoring_writer" {
  project = var.project_id
  role    = "roles/monitoring.metricWriter"
  member  = "serviceAccount:${google_service_account.workload.email}"
}

# Workload SA can write logs to Cloud Logging
resource "google_project_iam_member" "workload_log_writer" {
  project = var.project_id
  role    = "roles/logging.logWriter"
  member  = "serviceAccount:${google_service_account.workload.email}"
}
