package com.wfp.workflow.repository;

import com.wfp.workflow.entity.FieldValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FieldValueRepository extends JpaRepository<FieldValue, UUID> {
    List<FieldValue> findByProcessInstanceIdAndTenantId(String processInstanceId, String tenantId);
    Optional<FieldValue> findByFieldSchemaIdAndProcessInstanceIdAndTenantId(
            UUID schemaId, String processInstanceId, String tenantId);
}
