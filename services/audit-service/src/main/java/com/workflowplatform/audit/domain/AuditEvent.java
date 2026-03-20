package com.workflowplatform.audit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit event record persisted from Kafka topics.
 *
 * NOTE: In production this table should be partitioned by month using
 * PostgreSQL declarative partitioning:
 *
 *   CREATE TABLE audit_event (...)
 *   PARTITION BY RANGE (occurred_at);
 *
 *   CREATE TABLE audit_event_2024_01
 *       PARTITION OF audit_event
 *       FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
 *
 * Partition creation should be managed by a scheduled job or pg_partman extension.
 */
@Entity
@Table(
    name = "audit_event",
    indexes = {
        @Index(name = "idx_audit_tenant_occurred", columnList = "tenant_id, occurred_at DESC"),
        @Index(name = "idx_audit_event_type", columnList = "event_type, occurred_at DESC"),
        @Index(name = "idx_audit_actor", columnList = "actor_id, occurred_at DESC"),
        @Index(name = "idx_audit_event_id", columnList = "event_id", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Original event ID from the source system (used for idempotency).
     */
    @Column(name = "event_id", nullable = false, length = 255, unique = true)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Column(name = "actor_id", length = 255)
    private String actorId;

    /**
     * When the event actually occurred in the source system.
     */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /**
     * When this audit record was persisted by the audit service.
     */
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    /**
     * Full raw event payload stored as JSONB for queryability.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    /**
     * Source Kafka topic (e.g., "workflow.events", "schema.events").
     */
    @Column(name = "topic", nullable = false, length = 255)
    private String topic;
}
