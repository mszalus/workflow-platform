package com.wfp.notification.controller;

import com.wfp.common.dto.PagedResponse;
import com.wfp.notification.dto.NotificationDto;
import com.wfp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public PagedResponse<NotificationDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        return notificationService.getNotifications(jwt.getClaimAsString("preferred_username"), page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("count", notificationService.getUnreadCount(jwt.getClaimAsString("preferred_username")));
    }

    @PutMapping("/mark-read")
    public void markRead(@RequestBody List<UUID> ids) {
        notificationService.markAsRead(ids);
    }

    @PutMapping("/mark-all-read")
    public void markAllRead(@AuthenticationPrincipal Jwt jwt) {
        notificationService.markAllAsRead(jwt.getClaimAsString("preferred_username"));
    }
}
