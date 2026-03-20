package com.workflowplatform.schema.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Defines a single field within a {@link FieldSchema}.
 * JSONB columns (validationRules, options) are mapped as String for portability.
 * visibleOnTasks and editableByRoles use PostgreSQL array type via JdbcTypeCode.
 */
@Entity
@Table(
    name = "field_definition",
    indexes = {
        @Index(name = "idx_field_def_schema_id", columnList = "schema_id"),
        @Index(name = "idx_field_def_key", columnList = "field_key")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schema_id", nullable = false)
    private FieldSchema schema;

    @Column(name = "field_key", nullable = false, length = 100)
    private String fieldKey;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 50)
    private FieldType fieldType;

    @Column(name = "required", nullable = false)
    private boolean required;

    /**
     * JSON object containing validation rules, e.g. min/max, pattern, etc.
     * Stored as PostgreSQL JSONB.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_rules", columnDefinition = "jsonb")
    private String validationRules;

    /**
     * JSON array of option objects for ENUM type fields.
     * Stored as PostgreSQL JSONB.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "jsonb")
    private String options;

    /**
     * Array of task definition keys on which this field is visible.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "visible_on_tasks", columnDefinition = "text[]")
    private String[] visibleOnTasks;

    /**
     * Array of role names that are allowed to edit this field.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "editable_by_roles", columnDefinition = "text[]")
    private String[] editableByRoles;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "placeholder", length = 500)
    private String placeholder;

    @Column(name = "help_text", length = 1000)
    private String helpText;
}
