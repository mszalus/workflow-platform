package com.workflowplatform.notification.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "notification",
    indexes = {
        @Index(name = "idx_notification_recipient", columnList = "recipient_id, is_read, created_at"),
        @Index(name = "idx_notification_tenant", columnList = "tenant_id, created_at"),
        @Index(name = "idx_notification_ref", columnList = "reference_id, reference_type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "recipient_id", nullable = false, length = 255)
    private String recipientId;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "body", columnDefinition = "text")
    private String body;

    @Column(name = "reference_id", length = 255)
    private String referenceId;

    @Column(name = "reference_type", length = 100)
    private String referenceType;

    @Column(name = "action_url", length = 1000)
    private String actionUrl;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "email_sent", nullable = false)
    @Builder.Default
    private boolean emailSent = false;

    @Column(name = "email_sent_at")
    private Instant emailSentAt;

    @Column(name = "source_event_id", length = 255)
    private String sourceEventId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
