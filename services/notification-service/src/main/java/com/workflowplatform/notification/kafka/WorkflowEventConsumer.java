package com.workflowplatform.notification.kafka;

import com.workflowplatform.notification.domain.Notification;
import com.workflowplatform.notification.domain.NotificationType;
import com.workflowplatform.notification.repository.NotificationRepository;
import com.workflowplatform.notification.service.EmailNotificationService;
import com.workflowplatform.notification.websocket.WebSocketNotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEventConsumer {

    private final NotificationRepository notificationRepository;
    private final EmailNotificationService emailNotificationService;
    private final WebSocketNotificationSender webSocketNotificationSender;

    @Value("${notification.base-url:http://localhost:3000}")
    private String baseUrl;

    @KafkaListener(
        topics = "${kafka.topics.workflow-events:workflow.events}",
        groupId = "${spring.kafka.consumer.group-id:notification-service}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(ConsumerRecord<String, Map<String, Object>> record) {
        Map<String, Object> event = record.value();
        if (event == null) {
            return;
        }

        String eventId = (String) event.get("eventId");
        String eventType = (String) event.get("eventType");

        // Idempotency check
        if (eventId != null && notificationRepository.existsBySourceEventId(eventId)) {
            log.debug("Skipping duplicate event id={}", eventId);
            return;
        }

        log.info("Processing event type={} id={}", eventType, eventId);

        try {
            switch (eventType != null ? eventType : "") {
                case "TASK_CREATED", "TASK_ASSIGNED" -> handleTaskAssigned(event);
                case "TASK_COMPLETED"               -> handleTaskCompleted(event);
                case "PROCESS_INSTANCE_STARTED"     -> handleProcessStarted(event);
                case "PROCESS_INSTANCE_COMPLETED"   -> handleProcessCompleted(event);
                case "PROCESS_INSTANCE_CANCELLED"   -> handleProcessCancelled(event);
                default -> log.debug("No handler for event type={}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing event type={} id={}", eventType, eventId, e);
            // Don't rethrow - allow Kafka to continue processing next records.
            // A dead-letter topic should be configured for production use.
        }
    }

    private void handleTaskAssigned(Map<String, Object> event) {
        String assignee = (String) event.get("assignee");
        if (assignee == null) return;

        String taskId = (String) event.get("taskId");
        String taskName = (String) event.get("taskName");
        String processInstanceId = (String) event.get("processInstanceId");
        String tenantId = (String) event.get("tenantId");
        String eventId = (String) event.get("eventId");

        String title = "New task assigned: " + (taskName != null ? taskName : taskId);
        String actionUrl = baseUrl + "/tasks/" + taskId;

        Notification notification = Notification.builder()
            .tenantId(tenantId)
            .recipientId(assignee)
            .notificationType(NotificationType.TASK_ASSIGNED)
            .title(title)
            .body("You have been assigned a new task. Please review and take action.")
            .referenceId(taskId)
            .referenceType("TASK")
            .actionUrl(actionUrl)
            .sourceEventId(eventId)
            .build();

        notificationRepository.save(notification);

        // Push via WebSocket to connected user
        webSocketNotificationSender.sendToUser(assignee, notification);

        // Send email asynchronously (recipient email will be resolved by notification service)
        emailNotificationService.sendTaskAssignedEmail(assignee, taskName, taskId, processInstanceId, actionUrl);
    }

    private void handleTaskCompleted(Map<String, Object> event) {
        String actorId = (String) event.get("actorId");
        String taskId = (String) event.get("taskId");
        String taskName = (String) event.get("taskName");
        String tenantId = (String) event.get("tenantId");
        String eventId = (String) event.get("eventId");

        if (actorId == null) return;

        Notification notification = Notification.builder()
            .tenantId(tenantId)
            .recipientId(actorId)
            .notificationType(NotificationType.TASK_COMPLETED)
            .title("Task completed: " + (taskName != null ? taskName : taskId))
            .body("The task has been successfully completed.")
            .referenceId(taskId)
            .referenceType("TASK")
            .sourceEventId(eventId)
            .build();

        notificationRepository.save(notification);
        webSocketNotificationSender.sendToUser(actorId, notification);
    }

    private void handleProcessStarted(Map<String, Object> event) {
        String actorId = (String) event.get("actorId");
        String processInstanceId = (String) event.get("processInstanceId");
        String processDefinitionKey = (String) event.get("processDefinitionKey");
        String tenantId = (String) event.get("tenantId");
        String eventId = (String) event.get("eventId");

        if (actorId == null) return;

        Notification notification = Notification.builder()
            .tenantId(tenantId)
            .recipientId(actorId)
            .notificationType(NotificationType.PROCESS_STARTED)
            .title("Process started: " + processDefinitionKey)
            .body("Process instance " + processInstanceId + " has been started.")
            .referenceId(processInstanceId)
            .referenceType("PROCESS_INSTANCE")
            .actionUrl(baseUrl + "/processes/" + processInstanceId)
            .sourceEventId(eventId)
            .build();

        notificationRepository.save(notification);
        webSocketNotificationSender.sendToUser(actorId, notification);
    }

    private void handleProcessCompleted(Map<String, Object> event) {
        String processInstanceId = (String) event.get("processInstanceId");
        String tenantId = (String) event.get("tenantId");
        String actorId = (String) event.get("actorId");
        String eventId = (String) event.get("eventId");

        if (actorId == null) return;

        Notification notification = Notification.builder()
            .tenantId(tenantId)
            .recipientId(actorId)
            .notificationType(NotificationType.PROCESS_COMPLETED)
            .title("Process completed")
            .body("Process instance " + processInstanceId + " has completed successfully.")
            .referenceId(processInstanceId)
            .referenceType("PROCESS_INSTANCE")
            .sourceEventId(eventId)
            .build();

        notificationRepository.save(notification);
        webSocketNotificationSender.sendToUser(actorId, notification);
    }

    private void handleProcessCancelled(Map<String, Object> event) {
        String processInstanceId = (String) event.get("processInstanceId");
        String tenantId = (String) event.get("tenantId");
        String actorId = (String) event.get("actorId");
        String eventId = (String) event.get("eventId");

        if (actorId == null) return;

        Notification notification = Notification.builder()
            .tenantId(tenantId)
            .recipientId(actorId)
            .notificationType(NotificationType.PROCESS_CANCELLED)
            .title("Process cancelled")
            .body("Process instance " + processInstanceId + " has been cancelled.")
            .referenceId(processInstanceId)
            .referenceType("PROCESS_INSTANCE")
            .sourceEventId(eventId)
            .build();

        notificationRepository.save(notification);
        webSocketNotificationSender.sendToUser(actorId, notification);
    }
}
