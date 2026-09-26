variable "project_id"  { type = string }
variable "region"      { type = string }
variable "environment" { type = string }
variable "network_id"  { type = string }
variable "tier"        { type = string; default = "db-g1-small" }
variable "ha_enabled"  { type = bool;   default = false }
variable "disk_gb"     { type = number; default = 20 }
variable "db_password" { type = string; sensitive = true }
