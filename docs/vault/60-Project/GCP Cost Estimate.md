---
title: GCP Cost Estimate
tags:
  - project
  - plan
  - status/open
type: plan
source: PLAN.md
status: open
---
[[Project MOC]] › **GCP Cost Estimate**

All prices approximate, us-central1 / europe-west1 regions, on-demand pricing (2026).

## Dev / Staging environment (minimal, one-shot testing)

| Resource | Spec | Monthly cost |
|---|---|---|
| GKE cluster management fee | 1 cluster (waived for first cluster per billing account) | $0–$73 |
| GKE nodes | 2 × e2-standard-2 (2 vCPU, 8 GB), Spot/preemptible | ~$25 |
| Cloud SQL | PostgreSQL 16, `db-f1-micro` (1 vCPU, 614 MB), no HA | ~$7 |
| Artifact Registry | ~5 GB image storage | ~$0.50 |
| Cloud Load Balancer | 1 L7 HTTP(S) LB | ~$18 |
| Cloud DNS | 1 managed zone | ~$0.50 |
| Cloud Logging | First 50 GB/month free | ~$0 |
| Cloud Trace | First 2.5M spans/month free | ~$0 |
| Cloud Monitoring | Free for GKE metrics | ~$0 |
| Network egress | ~10 GB/month | ~$1 |
| **Total (with free GKE mgmt)** | | **~$52/month** |
| **Total (without free tier)** | | **~$125/month** |

> **Note:** GCP gives new accounts $300 free credit (~3–6 months of dev environment).

## Production environment (minimal, 2 replicas, HA)

| Resource | Spec | Monthly cost |
|---|---|---|
| GKE cluster management | 1 cluster | ~$73 |
| GKE nodes | 3 × n2-standard-4 (4 vCPU, 16 GB), regular | ~$360 |
| Cloud SQL | PostgreSQL 16, `db-n1-standard-2` (2 vCPU, 7.5 GB), HA | ~$185 |
| Artifact Registry | ~20 GB | ~$0.40 |
| Cloud Load Balancer | 1 L7 HTTPS LB | ~$18 |
| Cloud Armor (WAF) | Basic tier | ~$5 |
| Cloud DNS | 1 zone | ~$0.50 |
| Cloud Logging | ~100 GB/month at scale | ~$25 |
| Cloud Trace | ~50M spans at moderate traffic | ~$25 |
| Cloud Monitoring | Custom metrics beyond free tier | ~$10 |
| Network egress | ~50 GB/month | ~$5 |
| **Total** | | **~$707/month** |

## Cost reduction levers

| Action | Saving |
|---|---|
| Use GKE Autopilot instead of Standard (for lower traffic) | 30–40% on node cost |
| Spot/preemptible nodes for non-prod workloads | 60–80% on node cost |
| Committed Use Discounts (1-year) | 37% on compute |
| Cloud SQL shared-core (`f1-micro`) in non-prod | Saves ~$175/month vs `n1-standard-2` |
| Scale down non-prod overnight (node pool min=0) | Saves ~60% on node cost |
| Use Cloud Run instead of GKE for stateless services | Pay only for requests, near-zero idle cost |

---

---


---

**Plan** — ← [[Step 17 — GCP Deployment and Acceptance Testing]] · [[Step 18 — Release and Rollback Strategy]] →

> [!abstract]- All notes in this set
> [[Active Tasks]]
> [[Project Context]]
> [[Step 01 — Fix Backend Dockerfiles]]
> [[Step 02 — Runtime test custom-fields-service and notification-service]]
> [[Step 03 — Gateway routing test]]
> [[Step 04 — Docker full-stack build and E2E]]
> [[Step 05 — README documentation]]
> [[Step 09 — Documentation User Manual and Admin Manual]]
> [[Step 10 — BPMN Import Export]]
> [[Step 06 — Helm deployment]]
> [[CI Pipeline Fixes]]
> [[Step 07 — Playwright E2E Tests]]
> [[Step 08 — Flowable BPMN Editor Extensions]]
> [[Frontend API Path Bug Fix]]
> [[Step 11 — Comprehensive E2E Testing and Bug Fixes]]
> [[Verification Log]]
> [[Step 12 — Architecture Diagrams and Data Model]]
> [[Step 13 — SDLC Improvements]]
> [[Step 14 — Observability and BDD Acceptance Tests]]
> [[Step 15 — Local Observability Verification]]
> [[Step 16 — GCP Infrastructure Terraform Helm Preparation]]
> [[Step 17 — GCP Deployment and Acceptance Testing]]
> [[Step 18 — Release and Rollback Strategy]]
> [[Step 19 — GCP Observability Readiness no deployment]]
> [[Step 20 — Work Item Tracker on BPMN]]
> [[Commit Strategy]]
