package com.wfp.workflow.service;

import com.wfp.security.context.TenantContext;
import com.wfp.workflow.dto.CommentDto;
import com.wfp.workflow.entity.Comment;
import com.wfp.workflow.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentDto addComment(String processInstanceId, String taskId, String userId, String content) {
        String tenantId = TenantContext.requireCurrentTenantId();
        Comment comment = Comment.builder()
                .processInstanceId(processInstanceId)
                .taskId(taskId)
                .userId(userId)
                .content(content)
                .tenantId(tenantId)
                .build();
        comment = commentRepository.save(comment);
        return toDto(comment);
    }

    public List<CommentDto> getComments(String processInstanceId) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return commentRepository.findByProcessInstanceIdAndTenantIdOrderByCreatedAtDesc(processInstanceId, tenantId)
                .stream().map(this::toDto).toList();
    }

    private CommentDto toDto(Comment c) {
        return CommentDto.builder()
                .id(c.getId() != null ? c.getId().toString() : null)
                .processInstanceId(c.getProcessInstanceId())
                .taskId(c.getTaskId())
                .userId(c.getUserId())
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
