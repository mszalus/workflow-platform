---
title: Build System
tags:
  - concept
  - gradle
  - build
type: concept
source: buildSrc/, settings.gradle
---

Gradle 9.2 with the **Groovy DSL** and three convention plugins in `buildSrc/`. No module
configures Java, Lombok or the Spring BOM itself.

| Plugin | Applies to | Provides |
|---|---|---|
| `wfp.java-conventions` | everything | Java 21 toolchain, UTF-8, JUnit 5 |
| `wfp.library-conventions` | `libs/*` | `java-library` + Lombok |
| `wfp.spring-boot-app` | `services/*` | Boot plugin + Lombok + Spring Cloud BOM |

## Commands

```bash
./gradlew build                                       # compile + test everything
./gradlew :services:workflow-service:bootJar -x test  # one service JAR
./gradlew :services:workflow-service:test             # one service test suite
```

> [!warning] Dockerfiles must copy the whole `services/` tree
> `settings.gradle` includes every module, so a build context missing a sibling service
> fails project evaluation — even though that sibling is not being built.
> See [[Known Pitfalls]].

> [!warning] `gradlew` needs the executable bit in git
> `git update-index --chmod=+x gradlew`, or CI fails with permission denied.

## See also

[[Build Commands|Backend build commands]] · [[CI Pipeline]] · [[Repository Layout]]
