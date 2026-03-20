package com.workflowplatform.notification.dto;

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
public class NotificationDto {

    private UUID id;
    private String userId;
    private String tenantId;
    private String type;
    private String title;
    private String body;
    private Instant readAt;
    private Instant createdAt;
    private String relatedEntityId;
    private String relatedEntityType;
    private boolean unread;
}
