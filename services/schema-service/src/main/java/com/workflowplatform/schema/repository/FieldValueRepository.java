package com.workflowplatform.schema.repository;

import com.workflowplatform.schema.domain.FieldType;
import com.workflowplatform.schema.domain.FieldValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FieldValueRepository extends JpaRepository<FieldValue, UUID> {

    List<FieldValue> findByTenantIdAndProcessInstanceId(String tenantId, String processInstanceId);

    List<FieldValue> findByTenantIdAndProcessInstanceIdAndTaskId(
        String tenantId, String processInstanceId, String taskId);

    Optional<FieldValue> findByTenantIdAndProcessInstanceIdAndTaskIdAndFieldKey(
        String tenantId, String processInstanceId, String taskId, String fieldKey);

    Optional<FieldValue> findByTenantIdAndProcessInstanceIdAndFieldKey(
        String tenantId, String processInstanceId, String fieldKey);

    Page<FieldValue> findByTenantIdAndProcessInstanceId(
        String tenantId, String processInstanceId, Pageable pageable);

    Page<FieldValue> findByTenantIdAndFieldKeyAndFieldType(
        String tenantId, String fieldKey, FieldType fieldType, Pageable pageable);

    @Modifying
    @Query("DELETE FROM FieldValue fv WHERE fv.tenantId = :tenantId AND fv.processInstanceId = :processInstanceId")
    int deleteByTenantIdAndProcessInstanceId(
        @Param("tenantId") String tenantId,
        @Param("processInstanceId") String processInstanceId
    );
}
