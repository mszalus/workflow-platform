output "gke_cluster_name" {
  description = "GKE cluster name"
  value       = module.gke.cluster_name
}

output "gke_cluster_endpoint" {
  description = "GKE cluster API endpoint (use gcloud to get credentials)"
  value       = module.gke.cluster_endpoint
  sensitive   = true
}

output "artifact_registry_url" {
  description = "Artifact Registry base URL for Docker images"
  value       = module.artifact_registry.registry_url
}

output "cloudsql_connection_name" {
  description = "Cloud SQL connection name for Cloud SQL Auth Proxy"
  value       = module.cloudsql.connection_name
}

output "cloudsql_private_ip" {
  description = "Cloud SQL private IP address (accessible from GKE pods)"
  value       = module.cloudsql.private_ip
}

output "workload_sa_email" {
  description = "GCP service account email used by Kubernetes workloads (Workload Identity)"
  value       = module.iam.workload_sa_email
}

output "workload_sa_key_hint" {
  description = "Reminder: no key file needed — Workload Identity is used"
  value       = "Workload Identity configured. Annotate K8s ServiceAccount with: iam.gke.io/gcp-service-account=${module.iam.workload_sa_email}"
}

output "kubeconfig_command" {
  description = "Command to configure kubectl"
  value       = "gcloud container clusters get-credentials ${module.gke.cluster_name} --region ${var.region} --project ${var.project_id}"
}

output "image_push_example" {
  description = "Example docker push command"
  value       = "docker push ${module.artifact_registry.registry_url}/workflow-service:$(git rev-parse --short HEAD)"
}
