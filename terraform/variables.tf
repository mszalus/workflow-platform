variable "project_id" {
  description = "GCP project ID"
  type        = string
}

variable "region" {
  description = "Primary GCP region"
  type        = string
  default     = "europe-west1"
}

variable "environment" {
  description = "Deployment environment (dev | prod)"
  type        = string
  validation {
    condition     = contains(["dev", "prod"], var.environment)
    error_message = "environment must be 'dev' or 'prod'."
  }
}

variable "cluster_name" {
  description = "GKE cluster name"
  type        = string
  default     = "wfp"
}

variable "node_machine_type" {
  description = "GCE machine type for GKE nodes"
  type        = string
  default     = "e2-standard-2"
}

variable "node_count_initial" {
  description = "Initial node count per zone"
  type        = number
  default     = 1
}

variable "node_count_min" {
  description = "Minimum nodes per zone (autoscaling)"
  type        = number
  default     = 1
}

variable "node_count_max" {
  description = "Maximum nodes per zone (autoscaling)"
  type        = number
  default     = 3
}

variable "node_spot" {
  description = "Use Spot (preemptible) nodes — recommended for non-prod to reduce cost"
  type        = bool
  default     = false
}

variable "sql_tier" {
  description = "Cloud SQL instance tier (e.g. db-f1-micro, db-g1-small, db-n1-standard-2)"
  type        = string
  default     = "db-g1-small"
}

variable "sql_ha" {
  description = "Enable Cloud SQL high-availability (regional) — adds ~$120/month"
  type        = bool
  default     = false
}

variable "sql_disk_gb" {
  description = "Cloud SQL storage in GB"
  type        = number
  default     = 20
}

variable "sql_db_password" {
  description = "PostgreSQL wfp user password (stored in Secret Manager after bootstrap)"
  type        = string
  sensitive   = true
}

variable "keycloak_admin_password" {
  description = "Keycloak admin console password"
  type        = string
  sensitive   = true
}

variable "rabbitmq_password" {
  description = "RabbitMQ wfp user password"
  type        = string
  sensitive   = true
}

variable "registry_location" {
  description = "Artifact Registry region (should match or be close to cluster region)"
  type        = string
  default     = "europe-west1"
}

variable "alert_email" {
  description = "Email address for Cloud Monitoring alert notifications"
  type        = string
  default     = ""
}
