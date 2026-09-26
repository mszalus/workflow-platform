---
title: Terraform — Prerequisites for GCP Deployment
tags:
  - ops
  - gcp
  - terraform
  - helm
type: report
source: terraform-helm-gcp-report.md
---
[[Operations MOC]] › **Terraform — Prerequisites for GCP Deployment**

Before running `terraform apply` or `helm install`:

1. **GCP project:** Create project, enable APIs:
   ```bash
   gcloud services enable container.googleapis.com \
     sqladmin.googleapis.com \
     secretmanager.googleapis.com \
     artifactregistry.googleapis.com \
     cloudtrace.googleapis.com \
     monitoring.googleapis.com
   ```

2. **GCS bucket for Terraform state:**
   ```bash
   gsutil mb -l europe-west1 gs://YOUR_PROJECT_ID-tfstate
   gsutil versioning set on gs://YOUR_PROJECT_ID-tfstate
   ```

3. **ESO installation** (before `helm install wfp`):
   ```bash
   helm repo add external-secrets https://charts.external-secrets.io
   helm install external-secrets external-secrets/external-secrets \
     -n external-secrets --create-namespace
   ```

4. **cert-manager** (for TLS):
   ```bash
   helm repo add jetstack https://charts.jetstack.io
   helm install cert-manager jetstack/cert-manager \
     -n cert-manager --create-namespace --set crds.enabled=true
   ```

---


---

**Terraform + Helm on GCP** — ← [[Terraform — What Was Built]] · [[Terraform — Validation Performed]] →

> [!abstract]- All notes in this set
> [[Terraform — What Was Built]]
> [[Terraform — Validation Performed]]
