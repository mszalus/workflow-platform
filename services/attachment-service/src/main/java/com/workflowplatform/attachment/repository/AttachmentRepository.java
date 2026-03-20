package com.workflowplatform.attachment.repository;

import com.workflowplatform.attachment.domain.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findByTenantIdAndProcessInstanceIdAndDeletedFalse(
        String tenantId, String processInstanceId);

    List<Attachment> findByTenantIdAndProcessInstanceIdAndTaskIdAndDeletedFalse(
        String tenantId, String processInstanceId, String taskId);

    List<Attachment> findByTenantIdAndProcessInstanceIdAndFieldKeyAndDeletedFalse(
        String tenantId, String processInstanceId, String fieldKey);

    Optional<Attachment> findByTenantIdAndIdAndDeletedFalse(String tenantId, UUID id);

    Optional<Attachment> findByObjectKey(String objectKey);

    boolean existsByTenantIdAndIdAndDeletedFalse(String tenantId, UUID id);
}
