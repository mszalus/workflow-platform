package com.wfp.workflow.repository;

import com.wfp.workflow.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    List<Attachment> findByProcessInstanceIdAndTenantId(String processInstanceId, String tenantId);
}
