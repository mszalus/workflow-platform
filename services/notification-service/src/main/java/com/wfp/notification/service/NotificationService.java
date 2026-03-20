package com.wfp.notification.service;

import com.wfp.common.dto.PagedResponse;
import com.wfp.notification.dto.NotificationDto;
import com.wfp.notification.entity.Notification;
import com.wfp.notification.entity.NotificationType;
import com.wfp.notification.repository.NotificationRepository;
import com.wfp.security.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void createNotification(String userId, String tenantId, String title,
                                    String message, NotificationType type,
                                    String referenceId, String referenceType) {
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .tenantId(tenantId)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build());
    }

    public PagedResponse<NotificationDto> getNotifications(String userId, int page, int size) {
        String tenantId = TenantContext.requireCurrentTenantId();
        Page<Notification> p = notificationRepository
                .findByUserIdAndTenantIdOrderByCreatedAtDesc(userId, tenantId, PageRequest.of(page, size));
        List<NotificationDto> items = p.getContent().stream().map(this::toDto).toList();
        return PagedResponse.of(items, page, size, p.getTotalElements());
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndTenantIdAndReadFalse(
                userId, TenantContext.requireCurrentTenantId());
    }

    @Transactional
    public void markAsRead(List<UUID> ids) {
        notificationRepository.markAsRead(ids, TenantContext.requireCurrentTenantId());
    }

    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsRead(userId, TenantContext.requireCurrentTenantId());
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId().toString())
                .userId(n.getUserId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .read(n.isRead())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
