package com.wfp.workflow.service;

import com.wfp.workflow.event.EventConstants;
import com.wfp.workflow.event.TaskCreatedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class EventPublisherTest {

    @Test
    void notifiesThenRecordsTheEvent() {
        NotificationService notificationService = mock(NotificationService.class);
        AuditService auditService = mock(AuditService.class);
        TaskCreatedEvent event = TaskCreatedEvent.builder().taskId("task-1").assignee("user-a").build();
        event.initDefaults(EventConstants.TASK_CREATED, "tenant-a", "user-a");

        new EventPublisher(notificationService, auditService).publish(event);

        InOrder order = inOrder(notificationService, auditService);
        order.verify(notificationService).notify(event);
        order.verify(auditService).record(event);
    }
}
