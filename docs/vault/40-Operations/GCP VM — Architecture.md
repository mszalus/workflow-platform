---
title: GCP VM — Architecture
tags:
  - ops
  - gcp
  - docker
type: runbook
source: deploy/gcp/README.md
---
[[Operations MOC]] › **GCP VM — Architecture**

```
Internet
   |
   v (Firewall: 5173, 5174, 8180, 9080)
+------------------------------+
|  e2-medium VM (wfp-vm)       |
|                              |
|  docker-compose              |
|  +- postgres:16              |
|  +- rabbitmq:3.13            |
|  +- keycloak:25              |
|  +- gateway         :9080    |
|  +- workflow-service :8081   |
|  +- custom-fields    :8082   |
|  +- notification     :8083   |
|  +- audit            :8084   |
|  +- admin-portal     :5173   |
|  +- user-portal      :5174   |
+------------------------------+
       |
       v
  Artifact Registry
  (us-central1-docker.pkg.dev)
```


---

**GCP VM runbook** — ← [[GCP VM — Key Deployment Notes]] · [[GCP VM — File Reference]] →

> [!abstract]- All notes in this set
> [[GCP VM — Prerequisites]]
> [[GCP VM — Deploy from Scratch]]
> [[GCP VM — Login Credentials]]
> [[GCP VM — Redeploy After Teardown]]
> [[GCP VM — Cost Control]]
> [[GCP VM — Updating After Code Changes]]
> [[GCP VM — Troubleshooting]]
> [[GCP VM — Key Deployment Notes]]
> [[GCP VM — File Reference]]
