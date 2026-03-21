package com.wfp.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.util.UUID;

@Entity
@Table(name = "notification_preference",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tenant_id", "event_type"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "email_enabled")
    @Builder.Default
    private boolean emailEnabled = true;

    @Column(name = "in_app_enabled")
    @Builder.Default
    private boolean inAppEnabled = true;
}
