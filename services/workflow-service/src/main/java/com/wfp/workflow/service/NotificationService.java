package com.wfp.workflow.service;

import com.wfp.common.dto.PagedResponse;
import com.wfp.events.BaseEvent;
import com.wfp.events.ProcessCompletedEvent;
import com.wfp.events.TaskAssignedEvent;
import com.wfp.events.TaskCompletedEvent;
import com.wfp.events.TaskCreatedEvent;
import com.wfp.security.context.TenantContext;
import com.wfp.workflow.dto.NotificationDto;
import com.wfp.workflow.entity.Notification;
import com.wfp.workflow.entity.NotificationType;
import com.wfp.workflow.repository.NotificationRepository;
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

    private static final int MAX_TITLE_LENGTH = 255;

    private final NotificationRepository notificationRepository;

    public void notify(BaseEvent event) {
        switch (event) {
            case TaskCreatedEvent e -> notifyTaskCreated(e);
            case TaskAssignedEvent e -> notifyTaskAssigned(e);
            case TaskCompletedEvent e -> notifyTaskCompleted(e);
            case ProcessCompletedEvent e -> notifyProcessCompleted(e);
            default -> { }
        }
    }

    public void createNotification(String userId, String tenantId, String title,
                                    String message, NotificationType type,
                                    String referenceId, String referenceType) {
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .tenantId(tenantId)
                .title(title.length() > MAX_TITLE_LENGTH ? title.substring(0, MAX_TITLE_LENGTH) : title)
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

    private void notifyTaskCreated(TaskCreatedEvent e) {
        if (e.getAssignee() != null) {
            createNotification(e.getAssignee(), e.getTenantId(),
                    "New Task: " + e.getTaskName(),
                    "You have been assigned a new task: " + e.getTaskName(),
                    NotificationType.TASK_ASSIGNED, e.getTaskId(), "TASK");
        }
    }

    private void notifyTaskAssigned(TaskAssignedEvent e) {
        if (e.getAssignee() != null) {
            createNotification(e.getAssignee(), e.getTenantId(),
                    "Task Assigned: " + e.getTaskName(),
                    "Task '" + e.getTaskName() + "' has been assigned to you",
                    NotificationType.TASK_ASSIGNED, e.getTaskId(), "TASK");
        }
    }

    private void notifyTaskCompleted(TaskCompletedEvent e) {
        String userId = e.getCompletedBy() != null ? e.getCompletedBy() : e.getUserId();
        if (userId != null) {
            createNotification(userId, e.getTenantId(),
                    "Task Completed: " + e.getTaskName(),
                    "Task '" + e.getTaskName() + "' has been completed",
                    NotificationType.TASK_COMPLETED, e.getTaskId(), "TASK");
        }
    }

    private void notifyProcessCompleted(ProcessCompletedEvent e) {
        if (e.getUserId() != null) {
            createNotification(e.getUserId(), e.getTenantId(),
                    "Process Completed: " + e.getProcessName(),
                    "Process '" + e.getProcessName() + "' has been completed",
                    NotificationType.PROCESS_COMPLETED, e.getProcessInstanceId(), "PROCESS");
        }
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
