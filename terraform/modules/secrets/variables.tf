variable "project_id"             { type = string }
variable "region"                 { type = string }
variable "environment"           { type = string }
variable "sql_db_password"       { type = string; sensitive = true }
variable "keycloak_admin_password" { type = string; sensitive = true }
variable "rabbitmq_password"     { type = string; sensitive = true }
variable "workload_sa_email"     { type = string }
