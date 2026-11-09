package com.wfp.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfp.security.context.TenantContext;
import com.wfp.workflow.entity.AuditEntry;
import com.wfp.workflow.repository.AuditEntryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditServiceTest {

    private final AuditEntryRepository auditEntryRepository = mock(AuditEntryRepository.class);
    private final AuditService auditService =
            new AuditService(auditEntryRepository, new ObjectMapper().findAndRegisterModules());

    @Test
    void recordsAnEventInTheCurrentTenantWithItsDetails() {
        TenantContext.runAs("tenant-a", () ->
                auditService.record("item.created", "ITEM", "PROJ-1", "user-b", Map.of("title", "Review")));

        ArgumentCaptor<AuditEntry> saved = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditEntryRepository).save(saved.capture());
        assertThat(saved.getValue())
                .extracting(AuditEntry::getEventType, AuditEntry::getEntityType, AuditEntry::getEntityId,
                        AuditEntry::getUserId, AuditEntry::getTenantId, AuditEntry::getSourceService)
                .containsExactly("item.created", "ITEM", "PROJ-1", "user-b", "tenant-a", "app");
        assertThat(saved.getValue().getDetails()).contains("\"title\":\"Review\"");
    }
}
