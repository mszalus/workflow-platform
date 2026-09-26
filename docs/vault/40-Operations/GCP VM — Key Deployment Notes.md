---
title: GCP VM — Key Deployment Notes
tags:
  - ops
  - gcp
  - docker
type: runbook
source: deploy/gcp/README.md
---
[[Operations MOC]] › **GCP VM — Key Deployment Notes**

- **VM size**: e2-medium (2 vCPU, 4 GB RAM) required. e2-small will OOM with 10 containers.
- **JWT issuer URI**: Auto-detected from VM metadata. [[Security and JWT|Keycloak]] tokens carry the external URL as `iss` claim, which must match the backend's configured issuer URI.
- **Nginx Origin header**: Frontend nginx proxies strip the `Origin` header to avoid CORS rejections at the gateway.
- **First boot**: Java services take 3-5 minutes to start due to Hibernate schema creation and [[Flowable Engine|Flowable engine]] initialization. Subsequent boots are faster (~90s).


---

**GCP VM runbook** — ← [[GCP VM — Troubleshooting]] · [[GCP VM — Architecture]] →

> [!abstract]- All notes in this set
> [[GCP VM — Prerequisites]]
> [[GCP VM — Deploy from Scratch]]
> [[GCP VM — Login Credentials]]
> [[GCP VM — Redeploy After Teardown]]
> [[GCP VM — Cost Control]]
> [[GCP VM — Updating After Code Changes]]
> [[GCP VM — Troubleshooting]]
> [[GCP VM — Architecture]]
> [[GCP VM — File Reference]]
