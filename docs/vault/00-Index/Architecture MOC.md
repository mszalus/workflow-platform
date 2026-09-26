---
title: Architecture MOC
tags:
  - moc
  - architecture
type: moc
source: docs/architecture/
---

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
