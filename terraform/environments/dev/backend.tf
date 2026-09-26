# Dev remote state configuration.
# To use: copy this file to terraform/backend-dev.tf, then run:
#   terraform init -backend-config=environments/dev/backend.tf
#
# Create the GCS bucket first:
#   gsutil mb -l europe-west1 gs://<project-id>-tfstate
#   gsutil versioning set on gs://<project-id>-tfstate

# bucket = "your-project-id-tfstate"
# prefix = "wfp/dev"
