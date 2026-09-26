provider "google" {
  project = var.project_id
  region  = var.region
}

provider "google-beta" {
  project = var.project_id
  region  = var.region
}

# ─── Networking ────────────────────────────────────────────────────────────────
module "networking" {
  source      = "./modules/networking"
  project_id  = var.project_id
  region      = var.region
  environment = var.environment
}

# ─── Artifact Registry ─────────────────────────────────────────────────────────
module "artifact_registry" {
  source             = "./modules/artifact-registry"
  project_id         = var.project_id
  registry_location  = var.registry_location
  environment        = var.environment
  workload_sa_email  = module.iam.workload_sa_email
  depends_on         = [module.iam]
}

# ─── IAM / Service Accounts ────────────────────────────────────────────────────
module "iam" {
  source      = "./modules/iam"
  project_id  = var.project_id
  environment = var.environment
}

# ─── Secret Manager ────────────────────────────────────────────────────────────
module "secrets" {
  source                  = "./modules/secrets"
  project_id              = var.project_id
  region                  = var.region
  environment             = var.environment
  sql_db_password         = var.sql_db_password
  keycloak_admin_password = var.keycloak_admin_password
  rabbitmq_password       = var.rabbitmq_password
  # Grant read access to the workload service account created in iam module
  workload_sa_email       = module.iam.workload_sa_email
}

# ─── Cloud SQL (PostgreSQL 16) ─────────────────────────────────────────────────
module "cloudsql" {
  source      = "./modules/cloudsql"
  project_id  = var.project_id
  region      = var.region
  environment = var.environment
  tier        = var.sql_tier
  ha_enabled  = var.sql_ha
  disk_gb     = var.sql_disk_gb
  db_password = var.sql_db_password
  network_id  = module.networking.vpc_id
  depends_on  = [module.networking]
}

# ─── GKE Cluster ───────────────────────────────────────────────────────────────
module "gke" {
  source              = "./modules/gke"
  project_id          = var.project_id
  region              = var.region
  environment         = var.environment
  cluster_name        = "${var.cluster_name}-${var.environment}"
  network             = module.networking.vpc_name
  subnetwork          = module.networking.subnet_name
  pods_range_name     = module.networking.pods_range_name
  services_range_name = module.networking.services_range_name
  node_machine_type   = var.node_machine_type
  node_count_initial  = var.node_count_initial
  node_count_min      = var.node_count_min
  node_count_max      = var.node_count_max
  node_spot           = var.node_spot
  workload_sa_email   = module.iam.workload_sa_email
  depends_on          = [module.networking, module.iam]
}
