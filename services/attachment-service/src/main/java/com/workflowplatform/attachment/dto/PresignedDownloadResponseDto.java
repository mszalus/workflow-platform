package com.workflowplatform.attachment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedDownloadResponseDto {

    private String downloadUrl;
    private String fileName;
    private String contentType;
    private Instant expiresAt;
}
