---
title: CI Pipeline
tags:
  - ops
type: reference
source: README.md
---
[[Operations MOC]] › **CI Pipeline**

GitHub Actions CI runs on every push to `main` and on pull requests:

1. **backend-build** — [[Build System|Gradle]] build + test (JDK 21)
2. **frontend-build** — npm install + TypeScript typecheck
3. **docker-build** — validates docker-compose config (only on main, after 1+2 pass)
4. **helm-lint** — lints all [[Helm and Kubernetes|Helm]] sub-charts


---

**Running and shipping** — ← [[Docker Compose Stack]] · [[Helm and Kubernetes]] →

> [!abstract]- All notes in this set
> [[Prerequisites]]
> [[Quick Start]]
> [[Local Development]]
> [[Tech Stack]]
> [[Repository Layout]]
> [[Build Commands]]
> [[Docker Compose Stack]]
> [[Helm and Kubernetes]]
> [[Observability Stack]]
> [[Ports and Endpoints]]
