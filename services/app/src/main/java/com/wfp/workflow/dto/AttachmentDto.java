package com.wfp.workflow.dto;

import java.time.Instant;
import java.util.UUID;

public record AttachmentDto(UUID id, String fileName, long sizeBytes, String uploadedBy, Instant uploadedAt) {
}
