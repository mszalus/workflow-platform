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
public class PresignedUploadResponseDto {

    private UUID attachmentId;
    private String uploadUrl;
    private Instant expiresAt;
}
