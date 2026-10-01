package com.wfp.workflow.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CommentDto {
    private String id;
    private String processInstanceId;
    private String taskId;
    private String userId;
    private String content;
    private Instant createdAt;
}
