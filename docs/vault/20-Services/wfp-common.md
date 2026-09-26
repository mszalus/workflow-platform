---
title: wfp-common
tags:
  - library
  - backend
type: library
source: libs/wfp-common
---

Cross-cutting response shapes and error handling. Consumed by all four backend services.

| Type | Purpose |
|---|---|
| `ErrorResponse` | Uniform error body (timestamp, status, message, path) |
| `PagedResponse<T>` | Page envelope used by every list endpoint |
| `GlobalExceptionHandler` | `@RestControllerAdvice` mapping exceptions to `ErrorResponse` |

Built with the `wfp.library-conventions` Gradle plugin — see [[Build System]].
