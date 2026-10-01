package com.wfp.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfp.workflow.entity.AuditEntry;
import com.wfp.workflow.event.EventConstants;
import com.wfp.workflow.event.TaskCompletedEvent;
import com.wfp.workflow.repository.AuditEntryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditServiceTest {

    private final AuditEntryRepository auditEntryRepository = mock(AuditEntryRepository.class);
    private final AuditService auditService =
            new AuditService(auditEntryRepository, new ObjectMapper().findAndRegisterModules());

    @Test
    void recordsATaskEventWithItsEntityAndDetails() {
        TaskCompletedEvent event = TaskCompletedEvent.builder().taskId("task-1").taskName("Review").completedBy("user-b").build();
        event.initDefaults(EventConstants.TASK_COMPLETED, "tenant-a", "user-b");

        auditService.record(event);

        ArgumentCaptor<AuditEntry> saved = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditEntryRepository).save(saved.capture());
        assertThat(saved.getValue())
                .extracting(AuditEntry::getEventType, AuditEntry::getEntityType, AuditEntry::getEntityId,
                        AuditEntry::getUserId, AuditEntry::getTenantId, AuditEntry::getSourceService)
                .containsExactly("task.completed", "TASK", "task-1", "user-b", "tenant-a", "app");
        assertThat(saved.getValue().getDetails()).contains("\"taskName\":\"Review\"");
    }
}
