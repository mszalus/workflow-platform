package com.workflowplatform.schema.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Stores the actual value of a field for a specific process instance / task combination.
 * Typed value columns allow efficient querying per field type.
 */
@Entity
@Table(
    name = "field_value",
    indexes = {
        @Index(name = "idx_field_value_tenant_proc", columnList = "tenant_id, process_instance_id"),
        @Index(name = "idx_field_value_tenant_task", columnList = "tenant_id, task_id"),
        @Index(name = "idx_field_value_tenant_key", columnList = "tenant_id, field_key"),
        @Index(name = "idx_field_value_proc_key", columnList = "process_instance_id, field_key")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_field_value_proc_task_key",
            columnNames = {"process_instance_id", "task_id", "field_key"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldValue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "process_instance_id", nullable = false, length = 255)
    private String processInstanceId;

    @Column(name = "task_id", length = 255)
    private String taskId;

    @Column(name = "field_key", nullable = false, length = 100)
    private String fieldKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 50)
    private FieldType fieldType;

    @Column(name = "value_text", columnDefinition = "text")
    private String valueText;

    @Column(name = "value_number", precision = 38, scale = 10)
    private BigDecimal valueNumber;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    /**
     * JSON value for complex / structured data (FILE_REF, USER_REF, etc.).
     * Stored as PostgreSQL JSONB.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value_json", columnDefinition = "jsonb")
    private String valueJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
