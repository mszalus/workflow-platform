---
title: shared-ui
tags:
  - library
  - frontend
type: library
source: frontend/packages/shared-ui
---

Shared React building blocks for both portals.

| Area | Contents |
|---|---|
| `api/apiClient.ts` | fetch wrapper: base URL, bearer token injection, error mapping |
| `auth/AuthProvider.tsx` | OIDC context — login redirect, token refresh, claims |
| `types/` | Shared DTO types mirroring the backend contracts |

Must be built **first** in the npm workspace — see [[Frontend Architecture]].
