#!/usr/bin/env python3
"""Operations extras, MOC hub notes, Home dashboard, screenshots and .obsidian config."""
import sys, pathlib, shutil, json
sys.path.insert(0, str(pathlib.Path(__file__).parent))
from build_vault import ROOT, VAULT, fm, write

def n(folder, name, tags, ntype, source, body):
    write(folder, name, fm(name, tags, ntype, source) + body.strip() + "\n")

# ------------------------------------------------------------- operations extras
n("40-Operations", "Build Commands", ["ops", "build"], "reference", "CLAUDE.md", """
## Backend — requires JDK 21

```bash
./gradlew build                                       # compile + test all modules
./gradlew :services:workflow-service:bootJar -x test  # single service JAR
./gradlew :services:workflow-service:test             # single service tests
```

## Frontend

```bash
cd frontend
npm ci                                       # clean install
npm run build                                # shared-ui → bpmn-editor → apps
npm run typecheck --workspaces --if-present
npm run dev:admin                            # admin-portal dev server
npm run dev:user                             # user-portal dev server
```

Build order is load-bearing — see [[Frontend Architecture]].

## Docker

```bash
docker compose -f docker/docker-compose.yml build
docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.yml logs <service>
docker compose -f docker/docker-compose.yml config --quiet   # validate
```

## Helm

```bash
helm lint helm/charts/<chart-name>/
helm dependency update helm/workflow-platform/
helm install wfp helm/workflow-platform/ -f helm/workflow-platform/values-local.yaml
```

## Before pushing

`./gradlew build` **and** `npm run typecheck` must both pass — see [[CI Pipeline]].

## See also

[[Build System]] · [[Quick Start]] · [[Local Development]] · [[Docker Compose Stack]]
""")

n("40-Operations", "Docker Compose Stack", ["ops", "docker"], "reference",
  "docker/docker-compose.yml", """
The full local stack: 5 backend containers, 2 frontends, 3 infrastructure containers and
4 observability containers.

## Containers

| Group | Containers |
|---|---|
| Data | `wfp-postgres` (PG 16), `wfp-rabbitmq` (3.13-management), `wfp-keycloak` (25.0.6) |
| Backend | `wfp-gateway`, `wfp-workflow`, `wfp-custom-fields`, `wfp-notification`, `wfp-audit` |
| Frontend | `wfp-admin-portal`, `wfp-user-portal` (nginx) |
| Observability | `wfp-otel-collector`, `wfp-tempo`, `wfp-prometheus`, `wfp-grafana` |

Port map: [[Ports and Endpoints]].

## Startup order

PostgreSQL and RabbitMQ come up first with healthchecks, then Keycloak (which needs its
own schema in PG), then the backend services, then the portals. `init-db.sql` creates all
five schemas on first boot of the PG volume.

> [!warning] Schemas are created once
> `init-db.sql` runs only when the PostgreSQL data volume is empty. Adding a schema later
> means either running the DDL by hand or removing the volume.

## Build context gotcha

Each backend Dockerfile copies the **entire** `services/` directory, because
`settings.gradle` includes every module and Gradle evaluates all of them. See
[[Known Pitfalls]].

## See also

[[Quick Start]] · [[Admin — Docker Deployment]] · [[Deployment — Docker Compose]] · [[Build Commands]]
""")

n("40-Operations", "Observability Stack", ["ops", "observability"], "reference",
  "docker/docker-compose.yml, observability-report.md", """
OpenTelemetry traces, Prometheus metrics and structured JSON logs, correlated by trace id.

```mermaid
flowchart LR
    SVC["Spring Boot services<br/>Micrometer + OTel"] -->|OTLP :4318| COL["OTel Collector"]
    COL --> TEMPO["Tempo :3200"]
    PROM["Prometheus :9090"] -->|scrape /actuator/prometheus| SVC
    PROM -->|scrape :8888| COL
    GRAF["Grafana :3000"] --> TEMPO
    GRAF --> PROM
```

## What each service exposes

`management.endpoints.web.exposure.include: health, info, prometheus, metrics`, every
metric tagged `application=<service name>`, tracing sampled at `1.0` and exported to
`${OTEL_EXPORTER_OTLP_ENDPOINT}/v1/traces`.

> [!note] Sampling is 100%
> Fine locally; lower `management.tracing.sampling.probability` before production.

## Trace–log correlation

Trace and span ids are placed in the MDC and emitted in the JSON log line, so a trace in
Grafana can be pivoted to the exact log entries. Verification detail:
[[Observability — 6 MDC Context Propagation]] and
[[Observability — 5 Full Observability Stack Verified]].

## See also

[[Observability — Summary]] · [[Observability — Architecture for GCP]] · [[Ports and Endpoints]]
""")

# ------------------------------------------------------------- screenshots
shots = sorted((ROOT / "docs" / "screenshots").glob("*.png"))
for s in shots:
    shutil.copy2(s, VAULT / "attachments" / s.name)

GROUPS = {
    "Admin Portal": ["admin-dashboard", "admin-process-list", "admin-process-designer",
                     "admin-process-designer-import", "admin-custom-fields",
                     "admin-audit-log", "import-review-sales-lead"],
    "User Portal": ["user-dashboard", "user-task-inbox", "user-task-inbox-updated",
                    "task-inbox-with-flowable-samples", "user-start-process",
                    "user-notifications"],
    "Infrastructure": ["keycloak-login", "rabbitmq-overview", "gateway-health"],
}
LINKS = {
    "admin-dashboard": "Admin — Admin Portal", "admin-process-list": "Admin — Managing Process Definitions",
    "admin-process-designer": "Admin — Process Designer",
    "admin-process-designer-import": "Admin — Process Designer",
    "import-review-sales-lead": "Admin — Process Designer",
    "admin-custom-fields": "Admin — Custom Field Schemas", "admin-audit-log": "Admin — Audit Log",
    "user-dashboard": "User — Dashboard", "user-task-inbox": "User — Task Inbox",
    "user-task-inbox-updated": "User — Task Inbox",
    "task-inbox-with-flowable-samples": "User — Task Inbox",
    "user-start-process": "User — Starting a New Process", "user-notifications": "User — Notifications",
    "keycloak-login": "Security and JWT", "rabbitmq-overview": "Admin — RabbitMQ Monitoring",
    "gateway-health": "API Gateway",
}
names = {s.stem for s in shots}
body = ["Captured from the running stack. Each shot links to the note that explains it.\n"]
used = set()
for group, keys in GROUPS.items():
    body.append(f"## {group}\n")
    for k in keys:
        if k not in names:
            continue
        used.add(k)
        body.append(f"### {k.replace('-', ' ').title()}\n")
        body.append(f"![[{k}.png]]\n")
        body.append(f"→ [[{LINKS[k]}]]\n")
leftover = sorted(names - used)
if leftover:
    body.append("## Other\n")
    for k in leftover:
        body.append(f"### {k}\n\n![[{k}.png]]\n")
n("00-Index", "Screenshot Gallery", ["index", "screenshots"], "gallery",
  "docs/screenshots/", "\n".join(body))

# ------------------------------------------------------------- MOCs
n("00-Index", "Architecture MOC", ["moc", "architecture"], "moc", "docs/architecture/", """
The C4 chain, top down. Each level zooms into the one above.

| Level | Note | Answers |
|---|---|---|
| L1 | [[C4 L1 System Context]] | Who uses the platform, what it talks to |
| L2 | [[C4 L2 Container]] | What is deployed, and how the pieces connect |
| L3 | [[C4 L3 API Gateway]] | Routing and security edge internals |
| L3 | [[C4 L3 Workflow Service]] | The core service internals |
| L3 | [[C4 L3 Notification Service]] | The event-driven consumer internals |
| L4 | [[Deployment Topologies]] | Where it runs: Docker, GCP, Kubernetes |
| ERD | [[Data Model ERD]] | Tables, relationships, schema boundaries |

Conventions for editing the diagrams: [[Diagram Conventions]].

## Not covered by a C4 L3 diagram

[[Custom Fields Service]] and [[Audit Service]] have no component diagram — they are small
enough that the service notes carry the detail.

## See also

[[Concepts MOC]] · [[Services MOC]] · [[Reference MOC]]
""")

n("00-Index", "Services MOC", ["moc", "services"], "moc", "services/, frontend/", """
## Backend

| Service | Port | Schema | Publishes | Consumes |
|---|---|---|---|---|
| [[API Gateway]] | 9080→8080 | — | — | — |
| [[Workflow Service]] | 8081 | `workflow` | all 7 events | — |
| [[Custom Fields Service]] | 8082 | `custom_fields` | — | — |
| [[Notification Service]] | 8083 | `notification` | — | `task.*`, `process.completed` |
| [[Audit Service]] | 8084 | `audit` | — | `#` (everything) |

## Frontend

[[Admin Portal]] (5173) · [[User Portal]] (5174)

## Shared libraries

| Backend | Frontend |
|---|---|
| [[wfp-common]] | [[shared-ui]] |
| [[wfp-events]] | [[bpmn-editor]] |
| [[wfp-security]] | |
| [[wfp-test-support]] | |

## See also

[[Ports and Endpoints]] · [[API Endpoint Catalog]] · [[Repository Layout]]
""")

n("00-Index", "Concepts MOC", ["moc", "concepts"], "moc", "30-Concepts", """
The nine ideas that explain why the code looks the way it does.

| Concept | One-line |
|---|---|
| [[Multi-Tenancy]] | Four enforcement layers, one `@FilterDef` per persistence unit |
| [[Event System]] | One producer, two consumers, a topic exchange, no sync calls |
| [[Security and JWT]] | Keycloak OIDC; every service re-validates independently |
| [[Gateway Routing]] | Two rewritten prefixes, two pass-throughs, and why |
| [[Flowable Engine]] | Embedded BPMN engine sharing the `workflow` schema |
| [[Frontend Architecture]] | npm workspaces with a load-bearing build order |
| [[Build System]] | Gradle convention plugins in `buildSrc/` |
| [[Testing Strategy]] | Testcontainers, MockMvc, Playwright, BDD |
| [[Known Pitfalls]] | The ten traps this repo actually hits |

> [!tip] Start here
> If you are new: [[C4 L2 Container]] → [[Multi-Tenancy]] → [[Event System]] → [[Known Pitfalls]].
""")

n("00-Index", "Reference MOC", ["moc", "reference"], "moc", "35-Reference", """
Generated from the source tree — regenerate rather than hand-edit when the code moves.

## API

[[API Endpoint Catalog]] — every route, external and internal
[[Ports and Endpoints]] — host ports and health URLs

## Events

[[Event Catalog]] — all routing keys and their status

[[process.started]] · [[process.completed]] · [[process.cancelled]]
[[task.created]] · [[task.assigned]] · [[task.completed]] · [[task.delegated]]

## Data

[[Entity Reference]] — all nine entities, tenancy posture, cross-schema edges
[[Enumerations]] — `FieldType`, `NotificationType`

[[ProcessMetadata]] · [[Comment]] · [[Attachment]]
[[FieldSchema]] · [[FieldOption]] · [[FieldValue]]
[[Notification]] · [[NotificationPreference]]
[[AuditEntry]]
""")

n("00-Index", "Operations MOC", ["moc", "ops"], "moc", "40-Operations", """
## Running it

[[Prerequisites]] · [[Quick Start]] · [[Local Development]] · [[Build Commands]]
[[Docker Compose Stack]] · [[Ports and Endpoints]]

## Deploying it

| Target | Notes |
|---|---|
| Docker Compose | [[Deployment — Docker Compose]] · [[Admin — Docker Deployment]] |
| Single GCP VM | [[GCP VM — Deploy from Scratch]] · [[GCP VM — Cost Control]] · [[GCP VM — Troubleshooting]] |
| Kubernetes / GKE | [[Helm and Kubernetes]] · [[Deployment — Kubernetes]] · [[Admin — Kubernetes Deployment]] |
| Terraform on GCP | [[Terraform — What Was Built]] · [[Terraform — Prerequisites for GCP Deployment]] |

## Observing it

[[Observability Stack]] · [[Observability — Summary]] · [[Observability — 2 Distributed Tracing]]
[[Observability — 3 Prometheus Metrics]] · [[Observability — Architecture for GCP]]

## Shipping it

[[CI Pipeline]] · [[Testing Strategy]] · [[Step 18 — Release and Rollback Strategy]]

## When it breaks

[[Known Pitfalls]] · [[Admin — Troubleshooting]] · [[GCP VM — Troubleshooting]]
""")

n("00-Index", "Manuals MOC", ["moc", "manual"], "moc", "docs/", """
## For end users — [[User Portal]]

[[User — Getting Started]] · [[User — Login]] · [[User — Dashboard]]
[[User — Task Inbox]] · [[User — Completing a Task]]
[[User — Starting a New Process]] · [[User — My Processes]]
[[User — Notifications]] · [[User — Multi-Tenant Isolation]] · [[User — Troubleshooting]]

## For administrators — [[Admin Portal]]

[[Admin — Overview]] · [[Admin — Architecture]] · [[Admin — Admin Portal]]
[[Admin — Process Designer]] · [[Admin — Managing Process Definitions]]
[[Admin — Custom Field Schemas]] · [[Admin — Audit Log]]

## For operators

[[Admin — Keycloak Administration]] · [[Admin — RabbitMQ Monitoring]]
[[Admin — Docker Deployment]] · [[Admin — Kubernetes Deployment]] · [[Admin — Troubleshooting]]

## Visual walkthrough

[[Screenshot Gallery]]
""")

n("00-Index", "Project MOC", ["moc", "project"], "moc", "PLAN.md", """
Execution history and forward plan, split out of `PLAN.md`.

[[Active Tasks]] · [[Project Context]] · [[Verification Log]] · [[Commit Strategy]]

## Delivered

[[Step 01 — Fix Backend Dockerfiles]] · [[Step 02 — Runtime test custom-fields-service and notification-service]]
[[Step 03 — Gateway routing test]] · [[Step 04 — Docker full-stack build and E2E]]
[[Step 05 — README documentation]] · [[Step 07 — Playwright E2E Tests]]
[[Step 08 — Flowable BPMN Editor Extensions]] · [[Step 09 — Documentation User Manual and Admin Manual]]
[[Step 10 — BPMN Import Export]] · [[Step 11 — Comprehensive E2E Testing and Bug Fixes]]
[[Step 12 — Architecture Diagrams and Data Model]] · [[Step 13 — SDLC Improvements]]
[[Step 14 — Observability and BDD Acceptance Tests]]
[[CI Pipeline Fixes]] · [[Frontend API Path Bug Fix]]

## Blocked

[[Step 06 — Helm deployment]] — no `helm` binary available locally

## Open

[[Step 15 — Local Observability Verification]] · [[Step 16 — GCP Infrastructure Terraform Helm Preparation]]
[[Step 17 — GCP Deployment and Acceptance Testing]] · [[Step 18 — Release and Rollback Strategy]]
[[GCP Cost Estimate]]

## Known gaps in the product

| Gap | Where |
|---|---|
| `Attachment` has no REST endpoint | [[Attachment]] |
| `NotificationPreference` is never consulted | [[NotificationPreference]] |
| No email delivery channel | [[Notification Service]] |
| `process.sla.breached` has no publisher | [[Event Catalog]] |
| `field.*` events declared but unused | [[Event Catalog]] |
| No frontend unit test framework | [[Testing Strategy]] |
""")

# ------------------------------------------------------------- Home
HOME = """
> [!abstract] Workflow Platform
> Multi-tenant BPMN workflow platform. Admins design workflows visually with bpmn-js and
> deploy them; end users complete tasks through an inbox. Every action is audited, custom
> fields attach to any process, notifications arrive in real time.
>
> Java 21 · Spring Boot 3.3.5 · Flowable 7.1 · PostgreSQL 16 · RabbitMQ 3.13 · Keycloak 25 · React 18

## Start here

| I want to… | Go to |
|---|---|
| Understand the system | [[C4 L2 Container]] → [[Architecture MOC]] |
| Understand a decision | [[Concepts MOC]] |
| Find an endpoint, event or table | [[Reference MOC]] |
| Run or deploy it | [[Operations MOC]] |
| Use the product | [[Manuals MOC]] |
| Know what is left to do | [[Project MOC]] |
| See what it looks like | [[Screenshot Gallery]] |

## The system in one diagram

```mermaid
flowchart TB
    AP["Admin Portal<br/>:5173"] --> GW
    UP["User Portal<br/>:5174"] --> GW
    AP -.->|OIDC| KC["Keycloak :8180"]
    UP -.->|OIDC| KC
    GW["API Gateway :9080"] -.->|JWK Set| KC
    GW -->|/api/workflow| WF["Workflow Service :8081"]
    GW -->|/api/fields| CF["Custom Fields :8082"]
    GW -->|/api/notifications| NS["Notification :8083"]
    GW -->|/api/audit| AS["Audit :8084"]
    WF -->|publish| MQ{{"RabbitMQ<br/>wfp.events"}}
    MQ -->|"task.*, process.completed"| NS
    MQ -->|"#"| AS
    WF --> PG[("PostgreSQL 16<br/>5 schemas")]
    CF --> PG
    NS --> PG
    AS --> PG
    KC --> PG
```

## The four things that will bite you

1. **One `@FilterDef` per persistence unit** — not per entity. [[Multi-Tenancy]]
2. **Frontend build order** — `shared-ui` → `bpmn-editor` → apps. [[Frontend Architecture]]
3. **Dockerfiles copy the whole `services/` tree** — `settings.gradle` includes every module. [[Build System]]
4. **Gateway paths are rewritten** — `/api/workflow/tasks`, not `/api/tasks`. [[Gateway Routing]]

Full list: [[Known Pitfalls]].

## Vault layout

| Folder | Contents |
|---|---|
| `00-Index` | Hub notes — start from one of these |
| `10-Architecture` | C4 diagrams and the ERD (Mermaid, renders natively) |
| `20-Services` | One note per deployable and per shared library |
| `30-Concepts` | The ideas behind the code |
| `35-Reference` | Generated from source: endpoints, events, entities, enums |
| `40-Operations` | Build, run, deploy, observe, troubleshoot |
| `50-Manuals` | Admin and end-user documentation |
| `60-Project` | Plan, status, gaps |

> [!info] Regenerating
> Notes tagged `type: reference` and the `20-Services` notes are derived from the source
> tree; their `source:` frontmatter names the files they came from. `50-Manuals` and
> `60-Project` are split from `docs/*.md` and `PLAN.md`.
"""

(VAULT / "Home.md").write_text(
    fm("Home", ["moc", "home"], "home") + HOME.strip() + "\n", encoding="utf-8")

# ------------------------------------------------------------- .obsidian config
OBS = VAULT / ".obsidian"
(OBS / "app.json").write_text(json.dumps({
    "attachmentFolderPath": "attachments",
    "newLinkFormat": "shortest",
    "useMarkdownLinks": False,
    "alwaysUpdateLinks": True,
    "showLineNumber": True,
    "readableLineLength": True,
    "strictLineBreaks": False,
    "defaultViewMode": "preview",
    "livePreview": True,
}, indent=2), encoding="utf-8")

(OBS / "appearance.json").write_text(json.dumps({
    "accentColor": "#7c6cf5",
    "theme": "system",
    "baseFontSize": 16,
}, indent=2), encoding="utf-8")

(OBS / "core-plugins.json").write_text(json.dumps([
    "file-explorer", "global-search", "switcher", "graph", "backlink", "outgoing-link",
    "tag-pane", "page-preview", "daily-notes", "templates", "note-composer",
    "command-palette", "editor-status", "bookmarks", "outline", "word-count",
    "file-recovery", "markdown-importer", "random-note", "properties",
], indent=2), encoding="utf-8")

(OBS / "community-plugins.json").write_text("[]", encoding="utf-8")

COLOR_GROUPS = [
    ("path:10-Architecture", "rgb(120,160,255)"),
    ("path:20-Services",     "rgb(90,200,150)"),
    ("path:30-Concepts",     "rgb(255,180,80)"),
    ("path:35-Reference",    "rgb(190,150,255)"),
    ("path:40-Operations",   "rgb(255,130,130)"),
    ("path:50-Manuals",      "rgb(130,220,235)"),
    ("path:60-Project",      "rgb(200,200,120)"),
    ("path:00-Index",        "rgb(255,255,255)"),
]
(OBS / "graph.json").write_text(json.dumps({
    "collapse-filter": False, "search": "", "showTags": False,
    "showAttachments": False, "hideUnresolved": True, "showOrphans": True,
    "collapse-color-groups": False,
    "colorGroups": [{"query": q, "color": {"a": 1, "rgb": c}} for q, c in COLOR_GROUPS],
    "collapse-display": False, "showArrow": True, "textFadeMultiplier": -0.8,
    "nodeSizeMultiplier": 1.3, "lineSizeMultiplier": 1,
    "collapse-forces": False, "centerStrength": 0.45, "repelStrength": 11,
    "linkStrength": 0.8, "linkDistance": 210, "scale": 0.75, "close": False,
}, indent=2), encoding="utf-8")

(OBS / "hotkeys.json").write_text(json.dumps({
    "graph:open": [{"modifiers": ["Mod"], "key": "G"}],
    "backlink:open": [{"modifiers": ["Mod", "Shift"], "key": "B"}],
}, indent=2), encoding="utf-8")

print("index + obsidian config written;", len(shots), "screenshots copied")
