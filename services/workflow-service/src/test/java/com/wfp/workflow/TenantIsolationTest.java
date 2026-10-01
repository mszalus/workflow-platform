package com.wfp.workflow;

import com.wfp.workflow.entity.AuditEntry;
import com.wfp.workflow.repository.AuditEntryRepository;
import com.wfp.security.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TenantIsolationTest {

    @Autowired
    private AuditEntryRepository auditEntryRepository;

    @AfterEach
    void deleteEntries() {
        TenantContext.runAs("tenant-a", auditEntryRepository::deleteAll);
        TenantContext.runAs("tenant-b", auditEntryRepository::deleteAll);
    }

    @Test
    void readsOnlyTheCurrentTenantsRowsOutsideARequestOrTransaction() {
        saveEntryAs("tenant-a");
        UUID tenantBEntryId = saveEntryAs("tenant-b");
        AtomicReference<List<String>> tenantsSeenByA = new AtomicReference<>();
        AtomicReference<Optional<AuditEntry>> tenantBEntrySeenByA = new AtomicReference<>();

        TenantContext.runAs("tenant-a", () -> {
            tenantsSeenByA.set(auditEntryRepository.findAll().stream().map(AuditEntry::getTenantId).distinct().toList());
            tenantBEntrySeenByA.set(auditEntryRepository.findById(tenantBEntryId));
        });

        assertThat(tenantsSeenByA.get()).containsExactly("tenant-a");
        assertThat(tenantBEntrySeenByA.get()).isEmpty();
    }

    @Test
    void deletesOnlyTheCurrentTenantsRows() {
        saveEntryAs("tenant-a");
        saveEntryAs("tenant-b");

        TenantContext.runAs("tenant-a", auditEntryRepository::deleteAll);

        TenantContext.runAs("tenant-b", () -> assertThat(auditEntryRepository.count()).isEqualTo(1));
    }

    @Test
    void failsWithoutTenant() {
        assertThatThrownBy(() -> auditEntryRepository.findAll()).hasStackTraceContaining("Tenant ID is not set");
    }

    private UUID saveEntryAs(String tenantId) {
        AtomicReference<UUID> id = new AtomicReference<>();
        TenantContext.runAs(tenantId, () -> id.set(auditEntryRepository.save(AuditEntry.builder()
                .eventType("process.started")
                .entityType("PROCESS")
                .entityId("proc-1")
                .userId("user-a")
                .tenantId(tenantId)
                .timestamp(Instant.now())
                .details("{}")
                .sourceService("workflow-service")
                .build()).getId()));
        return id.get();
    }
}
