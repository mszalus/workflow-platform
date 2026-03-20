package com.workflowplatform.scheduler.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "scheduled_process",
    indexes = {
        @Index(name = "idx_scheduled_process_tenant", columnList = "tenant_id, enabled"),
        @Index(name = "idx_scheduled_process_key", columnList = "process_def_key")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_scheduled_process_tenant_name",
            columnNames = {"tenant_id", "name"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledProcess {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "process_def_key", nullable = false, length = 255)
    private String processDefKey;

    /**
     * Standard Quartz/Unix cron expression, e.g. "0 0 9 * * MON-FRI"
     */
    @Column(name = "cron_expression", nullable = false, length = 120)
    private String cronExpression;

    /**
     * JSONB: process variables to pass when starting the instance.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variables", columnDefinition = "jsonb")
    private String variables;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /**
     * Quartz job/trigger group name derived from tenantId.
     */
    @Column(name = "quartz_group", length = 100)
    private String quartzGroup;

    /**
     * Quartz job/trigger name (typically the UUID as a string).
     */
    @Column(name = "quartz_job_name", length = 255)
    private String quartzJobName;

    @Column(name = "last_fired_at")
    private Instant lastFiredAt;

    @Column(name = "next_fire_at")
    private Instant nextFireAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
