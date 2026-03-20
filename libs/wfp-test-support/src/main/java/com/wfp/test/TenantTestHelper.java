package com.wfp.test;

import com.wfp.security.context.TenantContext;

public final class TenantTestHelper {

    public static final String TEST_TENANT_A = "tenant-a";
    public static final String TEST_TENANT_B = "tenant-b";

    private TenantTestHelper() {}

    public static void setTenant(String tenantId) {
        TenantContext.setCurrentTenantId(tenantId);
    }

    public static void setTenantA() {
        setTenant(TEST_TENANT_A);
    }

    public static void setTenantB() {
        setTenant(TEST_TENANT_B);
    }

    public static void clearTenant() {
        TenantContext.clear();
    }
}
