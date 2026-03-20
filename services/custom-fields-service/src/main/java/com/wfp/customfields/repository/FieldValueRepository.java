package com.wfp.customfields.repository;

import com.wfp.customfields.entity.FieldValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FieldValueRepository extends JpaRepository<FieldValue, UUID> {
    List<FieldValue> findByProcessInstanceIdAndTenantId(String processInstanceId, String tenantId);
    List<FieldValue> findByProcessInstanceIdAndTaskIdAndTenantId(String processInstanceId, String taskId, String tenantId);
    Optional<FieldValue> findByFieldSchemaIdAndProcessInstanceIdAndTaskIdAndTenantId(UUID schemaId, String processInstanceId, String taskId, String tenantId);
    Optional<FieldValue> findByFieldSchemaIdAndProcessInstanceIdAndTaskIdIsNullAndTenantId(UUID schemaId, String processInstanceId, String tenantId);
}
