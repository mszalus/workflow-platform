package com.wfp.customfields.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "field_value")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FieldValue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "field_schema_id", nullable = false)
    private UUID fieldSchemaId;

    @Column(name = "process_instance_id", nullable = false)
    private String processInstanceId;

    @Column(columnDefinition = "TEXT")
    private String value;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
