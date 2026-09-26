variable "project_id" { type = string }
variable "region"     { type = string }
variable "environment" { type = string }

variable "subnet_cidr" {
  description = "Primary subnet CIDR for GKE nodes"
  type        = string
  default     = "10.0.0.0/20"
}

variable "pods_cidr" {
  description = "Secondary range CIDR for GKE pods (must not overlap subnet or services)"
  type        = string
  default     = "10.1.0.0/16"
}

variable "services_cidr" {
  description = "Secondary range CIDR for GKE services"
  type        = string
  default     = "10.2.0.0/20"
}
