variable "project_id"          { type = string }
variable "region"              { type = string }
variable "environment"         { type = string }
variable "cluster_name"        { type = string }
variable "network"             { type = string }
variable "subnetwork"          { type = string }
variable "pods_range_name"     { type = string }
variable "services_range_name" { type = string }
variable "workload_sa_email"   { type = string }
variable "node_machine_type"   { type = string; default = "e2-standard-2" }
variable "node_count_initial"  { type = number; default = 1 }
variable "node_count_min"      { type = number; default = 1 }
variable "node_count_max"      { type = number; default = 3 }
variable "node_spot"           { type = bool;   default = false }
