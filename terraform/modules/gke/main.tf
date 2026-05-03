# GKE Standard cluster — private nodes, Workload Identity, auto-upgrade
# Uses VPC-native networking (alias IP) and regional control plane for HA.

resource "google_container_cluster" "primary" {
  provider = google-beta
  name     = var.cluster_name
  location = var.region  # regional = 3-zone control plane
  project  = var.project_id

  # We manage the node pool separately to allow independent updates
  remove_default_node_pool = true
  initial_node_count       = 1

  network    = var.network
  subnetwork = var.subnetwork

  # VPC-native (alias IP): required for private clusters and GKE network policies
  networking_mode = "VPC_NATIVE"

  ip_allocation_policy {
    cluster_secondary_range_name  = var.pods_range_name
    services_secondary_range_name = var.services_range_name
  }

  # Private cluster: nodes and master have no external IPs
  private_cluster_config {
    enable_private_nodes    = true
    enable_private_endpoint = false  # Keep master endpoint public so gcloud/CI can connect
    master_ipv4_cidr_block  = "172.16.0.0/28"
  }

  # Workload Identity: pods can impersonate GCP service accounts without key files
  workload_identity_config {
    workload_pool = "${var.project_id}.svc.id.goog"
  }

  # Enable Cloud Logging and Monitoring for the cluster itself
  logging_config {
    enable_components = ["SYSTEM_COMPONENTS", "WORKLOADS"]
  }

  monitoring_config {
    enable_components = ["SYSTEM_COMPONENTS"]
    managed_prometheus {
      enabled = true  # Google Managed Prometheus — scrapes prometheus.io/scrape annotations
    }
  }

  # Maintenance window: off-peak for the primary deployment region (UTC)
  maintenance_policy {
    recurring_window {
      start_time = "2024-01-01T02:00:00Z"
      end_time   = "2024-01-01T06:00:00Z"
      recurrence = "FREQ=WEEKLY;BYDAY=SU"
    }
  }

  # Allow the control plane to reach nodes (required for private clusters)
  master_authorized_networks_config {
    cidr_blocks {
      cidr_block   = "0.0.0.0/0"
      display_name = "all-for-ci"
      # Tighten to your CI/CD runner IPs and office IPs in production
    }
  }

  release_channel {
    channel = "REGULAR"  # Auto-upgrades on stable releases; use STABLE for prod
  }
}

# Primary node pool — general workloads
resource "google_container_node_pool" "primary_nodes" {
  name     = "primary"
  location = var.region
  cluster  = google_container_cluster.primary.name
  project  = var.project_id

  initial_node_count = var.node_count_initial

  autoscaling {
    min_node_count = var.node_count_min
    max_node_count = var.node_count_max
  }

  management {
    auto_repair  = true
    auto_upgrade = true
  }

  node_config {
    machine_type = var.node_machine_type
    disk_type    = "pd-balanced"
    disk_size_gb = 50

    # Spot nodes: ~80% cheaper, but can be preempted (fine for stateless services)
    spot = var.node_spot

    # Workload Identity: required so pod service accounts can call GCP APIs
    workload_metadata_config {
      mode = "GKE_METADATA"
    }

    # OAuth scopes: minimal — Workload Identity handles API auth
    oauth_scopes = [
      "https://www.googleapis.com/auth/cloud-platform",
    ]

    service_account = var.workload_sa_email

    labels = {
      environment = var.environment
      managed-by  = "terraform"
    }

    shielded_instance_config {
      enable_secure_boot          = true
      enable_integrity_monitoring = true
    }
  }

  upgrade_settings {
    max_surge       = 1  # One extra node during rolling upgrades
    max_unavailable = 0  # Never take a node offline without a replacement
  }
}
