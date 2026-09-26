# VPC, subnets, secondary IP ranges (for GKE pods/services), Cloud NAT
# All resources are private: nodes, pods, and Cloud SQL have no public IPs.

locals {
  vpc_name          = "wfp-${var.environment}"
  subnet_name       = "wfp-${var.environment}-${var.region}"
  pods_range_name   = "pods"
  services_range_name = "services"
}

resource "google_compute_network" "vpc" {
  name                    = local.vpc_name
  auto_create_subnetworks = false
  project                 = var.project_id
}

resource "google_compute_subnetwork" "subnet" {
  name          = local.subnet_name
  ip_cidr_range = var.subnet_cidr       # e.g. 10.0.0.0/20
  region        = var.region
  network       = google_compute_network.vpc.id
  project       = var.project_id

  # Private Google Access: pods can reach GCP APIs (Cloud Trace, Secret Manager, etc.)
  # without going through Cloud NAT or the internet
  private_ip_google_access = true

  # Secondary ranges used by GKE for Pod and Service IPs
  secondary_ip_range {
    range_name    = local.pods_range_name
    ip_cidr_range = var.pods_cidr      # e.g. 10.1.0.0/16
  }

  secondary_ip_range {
    range_name    = local.services_range_name
    ip_cidr_range = var.services_cidr  # e.g. 10.2.0.0/20
  }
}

# Cloud NAT: allows pods to initiate outbound internet connections
# (needed for pulling images during init, reaching external APIs, etc.)
resource "google_compute_router" "router" {
  name    = "wfp-${var.environment}-router"
  region  = var.region
  network = google_compute_network.vpc.id
  project = var.project_id
}

resource "google_compute_router_nat" "nat" {
  name                               = "wfp-${var.environment}-nat"
  router                             = google_compute_router.router.name
  region                             = var.region
  project                            = var.project_id
  nat_ip_allocate_option             = "AUTO_ONLY"
  source_subnetwork_ip_ranges_to_nat = "ALL_SUBNETWORKS_ALL_IP_RANGES"

  log_config {
    enable = true
    filter = "ERRORS_ONLY"
  }
}

# VPC peering to enable private Cloud SQL connectivity
resource "google_compute_global_address" "sql_peering_range" {
  name          = "wfp-${var.environment}-sql-peering"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 20
  network       = google_compute_network.vpc.id
  project       = var.project_id
}

resource "google_service_networking_connection" "sql_peering" {
  network                 = google_compute_network.vpc.id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.sql_peering_range.name]
}

# Allow internal traffic within the cluster (required for pod-to-pod and health checks)
resource "google_compute_firewall" "allow_internal" {
  name    = "wfp-${var.environment}-allow-internal"
  network = google_compute_network.vpc.name
  project = var.project_id

  allow {
    protocol = "tcp"
  }
  allow {
    protocol = "udp"
  }
  allow {
    protocol = "icmp"
  }

  source_ranges = [var.subnet_cidr, var.pods_cidr, var.services_cidr]
}
