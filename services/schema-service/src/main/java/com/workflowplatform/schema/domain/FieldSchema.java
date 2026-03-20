package com.workflowplatform.schema.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Root aggregate for a versioned field schema associated with a process definition key.
 * A tenant may have multiple schema versions per processDefKey; only one may be active.
 */
@Entity
@Table(
    name = "field_schema",
    indexes = {
        @Index(name = "idx_field_schema_tenant_key_version", columnList = "tenant_id, process_def_key, version"),
        @Index(name = "idx_field_schema_tenant_active", columnList = "tenant_id, is_active")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_field_schema_tenant_key_version",
            columnNames = {"tenant_id", "process_def_key", "version"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "process_def_key", nullable = false, length = 255)
    private String processDefKey;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "description", length = 1000)
    private String description;

    @OneToMany(
        mappedBy = "schema",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<FieldDefinition> fields = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void addField(FieldDefinition field) {
        fields.add(field);
        field.setSchema(this);
    }

    public void removeField(FieldDefinition field) {
        fields.remove(field);
        field.setSchema(null);
    }
}
