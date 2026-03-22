#!/usr/bin/env bash
set -euo pipefail

PROJECT_ID=$(gcloud config get-value project 2>/dev/null)
REGION="us-central1"
ZONE="${REGION}-a"
VM_NAME="wfp-vm"
REPO_NAME="wfp-images"

echo "=== Tearing down Workflow Platform from GCP ==="
echo "Project: $PROJECT_ID"
echo ""
read -p "This will DELETE the VM and all data. Continue? (y/N) " confirm
if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
  echo "Aborted."
  exit 0
fi

echo ">>> Deleting VM..."
gcloud compute instances delete "$VM_NAME" --zone="$ZONE" --quiet 2>/dev/null || echo "  VM not found"

echo ">>> Deleting firewall rule..."
gcloud compute firewall-rules delete allow-wfp --quiet 2>/dev/null || echo "  Rule not found"

echo ">>> Deleting scheduler job..."
gcloud scheduler jobs delete wfp-auto-stop --location="$REGION" --quiet 2>/dev/null || echo "  Job not found"

echo ">>> Deleting service account..."
SA_EMAIL="wfp-scheduler@${PROJECT_ID}.iam.gserviceaccount.com"
gcloud iam service-accounts delete "$SA_EMAIL" --quiet 2>/dev/null || echo "  SA not found"

read -p "Delete Artifact Registry images too? (y/N) " del_images
if [[ "$del_images" == "y" || "$del_images" == "Y" ]]; then
  echo ">>> Deleting Artifact Registry repository..."
  gcloud artifacts repositories delete "$REPO_NAME" --location="$REGION" --quiet 2>/dev/null || echo "  Repo not found"
fi

echo ""
echo "=== Teardown complete ==="
