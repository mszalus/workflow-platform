package com.wfp.audit.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfp.audit.entity.AuditEntry;
import com.wfp.audit.service.AuditService;
import com.wfp.events.BaseEvent;
import com.wfp.events.EventConstants;
import com.wfp.events.ProcessCancelledEvent;
import com.wfp.events.ProcessCompletedEvent;
import com.wfp.events.ProcessStartedEvent;
import com.wfp.events.TaskAssignedEvent;
import com.wfp.events.TaskCompletedEvent;
import com.wfp.events.TaskCreatedEvent;
import com.wfp.events.TaskDelegatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = EventConstants.AUDIT_QUEUE)
    public void handleEvent(BaseEvent event) {
        log.info("Audit: received [{}] for tenant [{}]", event.getEventType(), event.getTenantId());

        String entityType = resolveEntityType(event);
        String entityId = resolveEntityId(event);
        String details = serializeEvent(event);

        auditService.saveEntry(AuditEntry.builder()
                .eventType(event.getEventType())
                .entityType(entityType)
                .entityId(entityId)
                .userId(event.getUserId())
                .tenantId(event.getTenantId())
                .timestamp(event.getTimestamp())
                .details(details)
                .sourceService("workflow-service")
                .build());
    }

    private String resolveEntityType(BaseEvent event) {
        return switch (event) {
            case ProcessStartedEvent e -> "PROCESS";
            case ProcessCompletedEvent e -> "PROCESS";
            case ProcessCancelledEvent e -> "PROCESS";
            case TaskCreatedEvent e -> "TASK";
            case TaskAssignedEvent e -> "TASK";
            case TaskCompletedEvent e -> "TASK";
            case TaskDelegatedEvent e -> "TASK";
            default -> "UNKNOWN";
        };
    }

    private String resolveEntityId(BaseEvent event) {
        return switch (event) {
            case ProcessStartedEvent e -> e.getProcessInstanceId();
            case ProcessCompletedEvent e -> e.getProcessInstanceId();
            case ProcessCancelledEvent e -> e.getProcessInstanceId();
            case TaskCreatedEvent e -> e.getTaskId();
            case TaskAssignedEvent e -> e.getTaskId();
            case TaskCompletedEvent e -> e.getTaskId();
            case TaskDelegatedEvent e -> e.getTaskId();
            default -> "unknown";
        };
    }

    private String serializeEvent(BaseEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Failed to serialize audit event", e);
            return "{}";
        }
    }
}
