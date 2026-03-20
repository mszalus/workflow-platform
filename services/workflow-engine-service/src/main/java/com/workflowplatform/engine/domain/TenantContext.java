package com.workflowplatform.engine.domain;

/**
 * ThreadLocal holder for the current tenant identifier.
 * Populated by the security filter after JWT validation.
 * Must be cleared at the end of every request to prevent thread pool leakage.
 */
public final class TenantContext {

    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();

    private TenantContext() {
        // utility class
    }

    /**
     * Store the tenant identifier for the current thread.
     *
     * @param tenantId the tenant identifier extracted from the JWT claim "tenant_id"
     */
    public static void setTenantId(String tenantId) {
        TENANT_ID.set(tenantId);
    }

    /**
     * Retrieve the tenant identifier for the current thread.
     *
     * @return the tenant identifier, or {@code null} if not set
     */
    public static String getTenantId() {
        return TENANT_ID.get();
    }

    /**
     * Clear the tenant identifier from the current thread.
     * Must be called in a {@code finally} block after each request.
     */
    public static void clear() {
        TENANT_ID.remove();
    }
}
