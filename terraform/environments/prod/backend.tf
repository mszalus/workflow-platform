# Prod remote state configuration.
# To use: copy this file to terraform/backend-prod.tf, then run:
#   terraform init -backend-config=environments/prod/backend.tf
#
# Create the GCS bucket first:
#   gsutil mb -l europe-west1 gs://<project-id>-tfstate-prod
#   gsutil versioning set on gs://<project-id>-tfstate-prod

# bucket = "your-project-id-tfstate-prod"
# prefix = "wfp/prod"
