package com.wfp.security.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTest {

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void runsActionAsTenantAndClearsAfterwards() {
        AtomicReference<String> tenantDuringAction = new AtomicReference<>();

        TenantContext.runAs("tenant-a", () -> tenantDuringAction.set(TenantContext.getCurrentTenantId()));

        assertThat(tenantDuringAction).hasValue("tenant-a");
        assertThat(TenantContext.getCurrentTenantId()).isNull();
    }

    @Test
    void clearsTenantWhenActionFails() {
        assertThatThrownBy(() -> TenantContext.runAs("tenant-a", () -> {
            throw new IllegalStateException("boom");
        })).hasMessage("boom");

        assertThat(TenantContext.getCurrentTenantId()).isNull();
    }
}
