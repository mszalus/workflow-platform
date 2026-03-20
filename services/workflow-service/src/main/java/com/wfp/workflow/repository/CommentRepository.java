package com.wfp.workflow.repository;

import com.wfp.workflow.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByProcessInstanceIdAndTenantIdOrderByCreatedAtDesc(String processInstanceId, String tenantId);
    List<Comment> findByTaskIdAndTenantIdOrderByCreatedAtDesc(String taskId, String tenantId);
}
