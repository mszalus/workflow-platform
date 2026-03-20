package com.workflowplatform.notification.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflowplatform.notification.domain.Notification;
import com.workflowplatform.notification.domain.NotificationPreference;
import com.workflowplatform.notification.domain.NotificationType;
import com.workflowplatform.notification.dto.NotificationDto;
import com.workflowplatform.notification.dto.NotificationPreferenceDto;
import com.workflowplatform.notification.repository.NotificationPreferenceRepository;
import com.workflowplatform.notification.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final ObjectMapper objectMapper;

    /**
     * Persist a new notification and return it.
     */
    public Notification createNotification(String userId,
                                           String tenantId,
                                           String type,
                                           String title,
                                           String body,
                                           String relatedEntityId,
                                           String relatedEntityType) {
        NotificationType notificationType;
        try {
            notificationType = NotificationType.valueOf(type);
        } catch (IllegalArgumentException e) {
            notificationType = NotificationType.GENERAL;
        }

        Notification notification = Notification.builder()
                .tenantId(tenantId)
                .recipientId(userId)
                .notificationType(notificationType)
                .title(title)
                .body(body)
                .referenceId(relatedEntityId)
                .referenceType(relatedEntityType)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.debug("Created notification id={} userId={} type={}", saved.getId(), userId, type);
        return saved;
    }

    /**
     * Return notifications for the user, optionally filtered to unread only.
     */
    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(String userId, String tenantId, boolean unreadOnly) {
        PageRequest pageRequest = PageRequest.of(0, 100, Sort.by("createdAt").descending());

        List<Notification> notifications = unreadOnly
                ? notificationRepository
                        .findByTenantIdAndRecipientIdAndIsReadFalseOrderByCreatedAtDesc(tenantId, userId, pageRequest)
                        .getContent()
                : notificationRepository
                        .findByTenantIdAndRecipientIdOrderByCreatedAtDesc(tenantId, userId, pageRequest)
                        .getContent();

        return notifications.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Mark a single notification as read. Validates ownership by userId and tenantId.
     */
    public void markAsRead(UUID id, String userId, String tenantId) {
        Notification notification = notificationRepository.findById(id)
                .filter(n -> tenantId.equals(n.getTenantId()) && userId.equals(n.getRecipientId()))
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + id));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
            log.debug("Marked notification id={} as read for userId={}", id, userId);
        }
    }

    /**
     * Mark all unread notifications as read and return the count updated.
     */
    public int markAllAsRead(String userId, String tenantId) {
        int count = notificationRepository.markAllAsRead(tenantId, userId, Instant.now());
        log.debug("Marked {} notifications as read for userId={}", count, userId);
        return count;
    }

    /**
     * Return the count of unread notifications for the user.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String userId, String tenantId) {
        return notificationRepository.countByTenantIdAndRecipientIdAndIsReadFalse(tenantId, userId);
    }

    /**
     * Return the notification preferences for the user, creating defaults if none exist.
     */
    @Transactional(readOnly = true)
    public NotificationPreferenceDto getPreferences(String userId, String tenantId) {
        return preferenceRepository.findByUserIdAndTenantId(userId, tenantId)
                .map(this::toPrefDto)
                .orElseGet(() -> NotificationPreferenceDto.builder()
                        .userId(userId)
                        .emailEnabled(true)
                        .inAppEnabled(true)
                        .build());
    }

    /**
     * Create or update notification preferences for the user.
     */
    public void updatePreferences(String userId, String tenantId, NotificationPreferenceDto dto) {
        NotificationPreference pref = preferenceRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseGet(() -> NotificationPreference.builder()
                        .userId(userId)
                        .tenantId(tenantId)
                        .build());

        pref.setEmailEnabled(dto.isEmailEnabled());
        pref.setInAppEnabled(dto.isInAppEnabled());

        if (dto.getPreferences() != null) {
            try {
                pref.setPreferences(objectMapper.writeValueAsString(dto.getPreferences()));
            } catch (Exception e) {
                log.warn("Failed to serialize preferences for userId={}", userId, e);
            }
        } else {
            pref.setPreferences(null);
        }

        preferenceRepository.save(pref);
        log.debug("Updated preferences for userId={}", userId);
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .userId(n.getRecipientId())
                .tenantId(n.getTenantId())
                .type(n.getNotificationType() != null ? n.getNotificationType().name() : null)
                .title(n.getTitle())
                .body(n.getBody())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .relatedEntityId(n.getReferenceId())
                .relatedEntityType(n.getReferenceType())
                .unread(!n.isRead())
                .build();
    }

    private NotificationPreferenceDto toPrefDto(NotificationPreference p) {
        Map<String, Object> prefsMap = null;
        if (p.getPreferences() != null && !p.getPreferences().isBlank()) {
            try {
                prefsMap = objectMapper.readValue(p.getPreferences(),
                        new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("Failed to deserialize preferences for userId={}", p.getUserId(), e);
            }
        }
        return NotificationPreferenceDto.builder()
                .userId(p.getUserId())
                .emailEnabled(p.isEmailEnabled())
                .inAppEnabled(p.isInAppEnabled())
                .preferences(prefsMap)
                .build();
    }
}
