---
title: Concepts MOC
tags:
  - moc
  - concepts
type: moc
source: 30-Concepts
---

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
