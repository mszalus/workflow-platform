package com.wfp.notification.dto;

import com.wfp.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class NotificationDto {
    private String id;
    private String userId;
    private String title;
    private String message;
    private NotificationType type;
    private boolean read;
    private String referenceId;
    private String referenceType;
    private Instant createdAt;
}
