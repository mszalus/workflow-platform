output "workload_sa_email" {
  value = google_service_account.workload.email
}

output "workload_sa_name" {
  value = google_service_account.workload.name
}

output "cicd_sa_email" {
  value = google_service_account.cicd.email
}

# Annotation value to put on the Kubernetes Service Account for Workload Identity
output "workload_identity_annotation" {
  value = google_service_account.workload.email
}
