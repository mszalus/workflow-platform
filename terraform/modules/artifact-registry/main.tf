# Artifact Registry — Docker repository for all service images.
# Images are tagged with git short SHA; :latest is also pushed for convenience.

resource "google_artifact_registry_repository" "wfp" {
  location      = var.registry_location
  repository_id = "wfp-${var.environment}"
  description   = "Workflow Platform Docker images (${var.environment})"
  format        = "DOCKER"
  project       = var.project_id

  # Keep up to 20 versions of each image — older ones are garbage-collected
  cleanup_policies {
    id     = "keep-20-versions"
    action = "KEEP"
    most_recent_versions {
      keep_count = 20
    }
  }

  # Remove any untagged image layers older than 14 days
  cleanup_policies {
    id     = "delete-untagged"
    action = "DELETE"
    condition {
      tag_state  = "UNTAGGED"
      older_than = "1209600s"  # 14 days
    }
  }
}

# Allow the GKE node service account to pull images from this registry
resource "google_artifact_registry_repository_iam_member" "gke_pull" {
  location   = google_artifact_registry_repository.wfp.location
  repository = google_artifact_registry_repository.wfp.name
  role       = "roles/artifactregistry.reader"
  member     = "serviceAccount:${var.workload_sa_email}"
  project    = var.project_id
}
