output "vpc_id"              { value = google_compute_network.vpc.id }
output "vpc_name"            { value = google_compute_network.vpc.name }
output "subnet_name"         { value = google_compute_subnetwork.subnet.name }
output "pods_range_name"     { value = local.pods_range_name }
output "services_range_name" { value = local.services_range_name }
output "sql_peering_id"      { value = google_service_networking_connection.sql_peering.id }
