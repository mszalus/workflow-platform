output "db_password_secret_id" {
  value = google_secret_manager_secret.db_password.secret_id
}

output "keycloak_admin_password_secret_id" {
  value = google_secret_manager_secret.keycloak_admin_password.secret_id
}

output "rabbitmq_password_secret_id" {
  value = google_secret_manager_secret.rabbitmq_password.secret_id
}
