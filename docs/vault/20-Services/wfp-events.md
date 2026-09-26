---
title: wfp-events
tags:
  - library
  - backend
type: library
source: libs/wfp-events
---

The event contract shared by publisher and consumers. Changing a class here is a
**wire-format change** affecting [[Workflow Service]], [[Notification Service]] and
[[Audit Service]] simultaneously.

- `BaseEvent` — abstract root: `eventId`, `eventType`, `tenantId`, `userId`, `timestamp`.
  Uses `@JsonTypeInfo(use = NAME, property = "eventType")` so the concrete type is
  recoverable on the consumer side.
- `EventConstants` — exchange name, every routing key, both queue names.
- Seven concrete event classes — see [[Event Catalog]].

Deserialization requires a `Jackson2JsonMessageConverter` bean in each consumer
`RabbitMQConfig`; without it Spring AMQP hands the listener a `byte[]`.
See [[Known Pitfalls]].
