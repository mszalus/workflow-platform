package com.wfp.workflow.service;

import com.wfp.workflow.event.BaseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final NotificationService notificationService;
    private final AuditService auditService;

    public void publish(BaseEvent event) {
        notificationService.notify(event);
        auditService.record(event);
    }
}
