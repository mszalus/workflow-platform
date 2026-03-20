package com.wfp.audit.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class AuditEntryDto {
    private String id;
    private String eventType;
    private String entityType;
    private String entityId;
    private String userId;
    private String tenantId;
    private Instant timestamp;
    private Map<String, Object> details;
    private String sourceService;
}
