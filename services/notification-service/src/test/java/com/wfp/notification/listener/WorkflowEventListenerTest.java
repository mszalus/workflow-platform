package com.wfp.notification.listener;

import com.wfp.events.EventConstants;
import com.wfp.events.TaskCreatedEvent;
import com.wfp.notification.service.NotificationService;
import com.wfp.security.context.TenantContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class WorkflowEventListenerTest {

    private final NotificationService notificationService = mock(NotificationService.class);
    private final WorkflowEventListener listener = new WorkflowEventListener(notificationService);

    @Test
    void createsNotificationAsTheEventTenant() {
        AtomicReference<String> tenantDuringCreate = new AtomicReference<>();
        doAnswer(invocation -> {
            tenantDuringCreate.set(TenantContext.getCurrentTenantId());
            return null;
        }).when(notificationService).createNotification(any(), any(), any(), any(), any(), any(), any());
        TaskCreatedEvent event = TaskCreatedEvent.builder().taskId("task-1").taskName("Review").assignee("user-a").build();
        event.initDefaults(EventConstants.TASK_CREATED, "tenant-a", "user-a");

        listener.handleEvent(event);

        assertThat(tenantDuringCreate).hasValue("tenant-a");
        assertThat(TenantContext.getCurrentTenantId()).isNull();
    }
}
