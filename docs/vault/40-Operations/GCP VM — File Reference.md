---
title: GCP VM — File Reference
tags:
  - ops
  - gcp
  - docker
type: runbook
source: deploy/gcp/README.md
---
[[Operations MOC]] › **GCP VM — File Reference**

| File | Purpose |
|------|---------|
| `setup.sh` | Full deployment: APIs, registry, images, VM, firewall, scheduler |
| `deploy-services.sh` | Run on VM: writes docker-compose, pulls images, starts services |
| `startup-script.sh` | Alternative VM startup script (for metadata-based startup) |
| `teardown.sh` | Deletes all GCP resources |
| `nginx-fix.conf` | Nginx config with Origin header stripping (for hot-fixing) |


---

**GCP VM runbook** — ← [[GCP VM — Architecture]]

> [!abstract]- All notes in this set
> [[GCP VM — Prerequisites]]
> [[GCP VM — Deploy from Scratch]]
> [[GCP VM — Login Credentials]]
> [[GCP VM — Redeploy After Teardown]]
> [[GCP VM — Cost Control]]
> [[GCP VM — Updating After Code Changes]]
> [[GCP VM — Troubleshooting]]
> [[GCP VM — Key Deployment Notes]]
> [[GCP VM — Architecture]]
