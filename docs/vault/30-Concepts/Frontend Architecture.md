---
title: Frontend Architecture
tags:
  - concept
  - frontend
  - react
  - build
type: concept
source: frontend/
---

An npm **workspaces** monorepo: two apps, two shared packages, one lockfile.

```
frontend/
├── packages/
│   ├── shared-ui/     → API client, AuthProvider, shared types
│   └── bpmn-editor/   → bpmn-js wrapper + Flowable property provider
└── apps/
    ├── admin-portal/  → depends on both packages
    └── user-portal/   → depends on shared-ui
```

## Build order is not optional

`shared-ui` → `bpmn-editor` → apps. The apps import the packages by workspace name and
resolve to built output, so a clean build that runs the apps first fails on missing
types. Recorded in [[Known Pitfalls]].

```bash
cd frontend
npm ci
npm run build                              # respects the order
npm run typecheck --workspaces --if-present
```

## Auth and API access

Both apps mount `AuthProvider` from [[shared-ui]], which performs the OIDC code flow
against [[Security and JWT|Keycloak]] and injects the bearer token into `apiClient`. All calls go through the
[[API Gateway]], so every path is prefixed per [[Gateway Routing]].

## Testing posture

TypeScript typecheck only — there is no frontend unit test framework yet.
Behaviour is covered by Playwright at the [[Testing Strategy|E2E layer]].

## See also

[[Admin Portal]] · [[User Portal]] · [[shared-ui]] · [[bpmn-editor]]
