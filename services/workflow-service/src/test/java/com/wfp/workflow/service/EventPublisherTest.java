package com.wfp.workflow.service;

import com.wfp.events.EventConstants;
import com.wfp.events.TaskCreatedEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EventPublisherTest {

    @Test
    void notifiesInProcessEvenWithoutRabbitMq() {
        NotificationService notificationService = mock(NotificationService.class);
        TaskCreatedEvent event = TaskCreatedEvent.builder().taskId("task-1").assignee("user-a").build();
        event.initDefaults(EventConstants.TASK_CREATED, "tenant-a", "user-a");

        new EventPublisher(null, notificationService).publish(EventConstants.TASK_CREATED, event);

        verify(notificationService).notify(event);
    }
}
