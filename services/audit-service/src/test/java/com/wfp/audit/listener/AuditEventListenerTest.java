package com.wfp.audit.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfp.audit.service.AuditService;
import com.wfp.events.EventConstants;
import com.wfp.events.TaskCreatedEvent;
import com.wfp.security.context.TenantContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class AuditEventListenerTest {

    private final AuditService auditService = mock(AuditService.class);
    private final AuditEventListener listener = new AuditEventListener(auditService, new ObjectMapper().findAndRegisterModules());

    @Test
    void savesEntryAsTheEventTenant() {
        AtomicReference<String> tenantDuringSave = new AtomicReference<>();
        doAnswer(invocation -> {
            tenantDuringSave.set(TenantContext.getCurrentTenantId());
            return null;
        }).when(auditService).saveEntry(any());
        TaskCreatedEvent event = TaskCreatedEvent.builder().taskId("task-1").build();
        event.initDefaults(EventConstants.TASK_CREATED, "tenant-a", "user-a");

        listener.handleEvent(event);

        assertThat(tenantDuringSave).hasValue("tenant-a");
        assertThat(TenantContext.getCurrentTenantId()).isNull();
    }
}
