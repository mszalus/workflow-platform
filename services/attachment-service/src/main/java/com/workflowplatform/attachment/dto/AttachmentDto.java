package com.workflowplatform.attachment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDto {

    private UUID id;
    private String tenantId;
    private String processInstanceId;
    private String taskId;
    private String fieldKey;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String objectKey;
    private String uploadedBy;
    private Instant uploadedAt;
    private boolean confirmed;
}
