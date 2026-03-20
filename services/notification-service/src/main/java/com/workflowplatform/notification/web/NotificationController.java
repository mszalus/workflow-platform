package com.workflowplatform.notification.web;

import com.workflowplatform.notification.domain.Notification;
import com.workflowplatform.notification.dto.NotificationDto;
import com.workflowplatform.notification.dto.NotificationPreferenceDto;
import com.workflowplatform.notification.repository.NotificationRepository;
import com.workflowplatform.notification.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    // ── Notification endpoints ──────────────────────────────────────────────

    /**
     * GET /api/v1/notifications?unreadOnly=false
     * Returns the caller's notifications, optionally filtered to unread only.
     */
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDto>> listNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @AuthenticationPrincipal Jwt jwt) {

        String userId   = jwt.getSubject();
        String tenantId = jwt.getClaimAsString("tenant_id");

        List<NotificationDto> notifications = notificationService.getNotifications(userId, tenantId, unreadOnly);
        return ResponseEntity.ok(notifications);
    }

    /**
     * PUT /api/v1/notifications/{id}/read
     */
    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String userId   = jwt.getSubject();
        String tenantId = jwt.getClaimAsString("tenant_id");

        notificationService.markAsRead(id, userId, tenantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/v1/notifications/read-all
     * Returns {"count": N} with the number of notifications marked as read.
     */
    @PutMapping("/notifications/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(
            @AuthenticationPrincipal Jwt jwt) {

        String userId   = jwt.getSubject();
        String tenantId = jwt.getClaimAsString("tenant_id");

        int count = notificationService.markAllAsRead(userId, tenantId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * GET /api/v1/notifications/unread-count
     */
    @GetMapping("/notifications/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal Jwt jwt) {

        String userId   = jwt.getSubject();
        String tenantId = jwt.getClaimAsString("tenant_id");

        long count = notificationService.getUnreadCount(userId, tenantId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    // ── Preference endpoints ────────────────────────────────────────────────

    /**
     * GET /api/v1/notification-preferences
     */
    @GetMapping("/notification-preferences")
    public ResponseEntity<NotificationPreferenceDto> getPreferences(
            @AuthenticationPrincipal Jwt jwt) {

        String userId   = jwt.getSubject();
        String tenantId = jwt.getClaimAsString("tenant_id");

        NotificationPreferenceDto dto = notificationService.getPreferences(userId, tenantId);
        return ResponseEntity.ok(dto);
    }

    /**
     * PUT /api/v1/notification-preferences
     */
    @PutMapping("/notification-preferences")
    public ResponseEntity<NotificationPreferenceDto> updatePreferences(
            @RequestBody NotificationPreferenceDto dto,
            @AuthenticationPrincipal Jwt jwt) {

        String userId   = jwt.getSubject();
        String tenantId = jwt.getClaimAsString("tenant_id");

        notificationService.updatePreferences(userId, tenantId, dto);
        NotificationPreferenceDto updated = notificationService.getPreferences(userId, tenantId);
        return ResponseEntity.ok(updated);
    }

    // ── Legacy paged endpoints (kept for backward-compat) ──────────────────

    /**
     * DELETE /api/v1/notifications/{id}
     */
    @DeleteMapping("/notifications/{id}")
    @Transactional
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        String userId   = jwt.getSubject();

        Notification notification = notificationRepository.findById(id)
                .filter(n -> n.getTenantId().equals(tenantId) && n.getRecipientId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + id));

        notificationRepository.delete(notification);
        return ResponseEntity.noContent().build();
    }
}
