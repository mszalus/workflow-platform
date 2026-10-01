package com.wfp.workflow.repository;

import com.wfp.workflow.entity.FieldSchema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FieldSchemaRepository extends JpaRepository<FieldSchema, UUID> {
    List<FieldSchema> findByProcessDefinitionKeyAndTenantIdOrderBySortOrder(String key, String tenantId);
    List<FieldSchema> findByTenantIdOrderBySortOrder(String tenantId);
    Optional<FieldSchema> findByIdAndTenantId(UUID id, String tenantId);
    Optional<FieldSchema> findByProcessDefinitionKeyAndFieldKeyAndTenantId(
            String pdKey, String fieldKey, String tenantId);
}
