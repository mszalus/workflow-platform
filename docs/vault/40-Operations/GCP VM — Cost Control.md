---
title: GCP VM — Cost Control
tags:
  - ops
  - gcp
  - docker
type: runbook
source: deploy/gcp/README.md
---
[[Operations MOC]] › **GCP VM — Cost Control**

## Option 1: Stop the VM ($0 compute, ~$1/mo disk)
```bash
# Stop
gcloud compute instances stop wfp-vm --zone=us-central1-a

# Restart (services auto-start via docker compose restart policy)
gcloud compute instances start wfp-vm --zone=us-central1-a
```

The auto-stop scheduler also stops the VM at midnight UTC daily. To disable:
```bash
gcloud scheduler jobs pause wfp-auto-stop --location=us-central1
```

## Option 2: Full teardown ($0 total)
```bash
cd deploy/gcp
bash teardown.sh
```

This deletes the VM, disk, firewall rule, scheduler job, and optionally the Artifact Registry.
All data (deployed processes, tasks) is lost. Redeploy with `bash setup.sh`.


---

**GCP VM runbook** — ← [[GCP VM — Redeploy After Teardown]] · [[GCP VM — Updating After Code Changes]] →

> [!abstract]- All notes in this set
> [[GCP VM — Prerequisites]]
> [[GCP VM — Deploy from Scratch]]
> [[GCP VM — Login Credentials]]
> [[GCP VM — Redeploy After Teardown]]
> [[GCP VM — Updating After Code Changes]]
> [[GCP VM — Troubleshooting]]
> [[GCP VM — Key Deployment Notes]]
> [[GCP VM — Architecture]]
> [[GCP VM — File Reference]]
