output "repository_url" {
  value = "${var.registry_location}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.wfp.repository_id}"
}

output "repository_name" {
  value = google_artifact_registry_repository.wfp.name
}
