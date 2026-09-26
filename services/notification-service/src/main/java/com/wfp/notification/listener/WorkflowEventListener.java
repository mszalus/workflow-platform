package com.wfp.notification.listener;

import com.wfp.events.BaseEvent;
import com.wfp.events.EventConstants;
import com.wfp.events.ProcessCompletedEvent;
import com.wfp.events.TaskAssignedEvent;
import com.wfp.events.TaskCompletedEvent;
import com.wfp.events.TaskCreatedEvent;
import com.wfp.notification.entity.NotificationType;
import com.wfp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = EventConstants.NOTIFICATION_QUEUE)
    public void handleEvent(BaseEvent event) {
        log.info("Received event: {} for tenant: {}", event.getEventType(), event.getTenantId());

        switch (event) {
            case TaskCreatedEvent e -> handleTaskCreated(e);
            case TaskAssignedEvent e -> handleTaskAssigned(e);
            case TaskCompletedEvent e -> handleTaskCompleted(e);
            case ProcessCompletedEvent e -> handleProcessCompleted(e);
            default -> log.debug("Unhandled event type: {}", event.getEventType());
        }
    }

    private void handleTaskCreated(TaskCreatedEvent e) {
        if (e.getAssignee() != null) {
            notificationService.createNotification(e.getAssignee(), e.getTenantId(),
                    "New Task: " + e.getTaskName(),
                    "You have been assigned a new task: " + e.getTaskName(),
                    NotificationType.TASK_ASSIGNED, e.getTaskId(), "TASK");
        }
    }

    private void handleTaskAssigned(TaskAssignedEvent e) {
        if (e.getAssignee() != null) {
            notificationService.createNotification(e.getAssignee(), e.getTenantId(),
                    "Task Assigned: " + e.getTaskName(),
                    "Task '" + e.getTaskName() + "' has been assigned to you",
                    NotificationType.TASK_ASSIGNED, e.getTaskId(), "TASK");
        }
    }

    private void handleTaskCompleted(TaskCompletedEvent e) {
        String userId = e.getCompletedBy() != null ? e.getCompletedBy() : e.getUserId();
        if (userId != null) {
            notificationService.createNotification(userId, e.getTenantId(),
                    "Task Completed: " + e.getTaskName(),
                    "Task '" + e.getTaskName() + "' has been completed",
                    NotificationType.TASK_COMPLETED, e.getTaskId(), "TASK");
        }
    }

    private void handleProcessCompleted(ProcessCompletedEvent e) {
        if (e.getUserId() != null) {
            notificationService.createNotification(e.getUserId(), e.getTenantId(),
                    "Process Completed: " + e.getProcessName(),
                    "Process '" + e.getProcessName() + "' has been completed",
                    NotificationType.PROCESS_COMPLETED, e.getProcessInstanceId(), "PROCESS");
        }
    }
}
