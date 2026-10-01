package com.wfp.security.filter;

import com.wfp.security.context.TenantContext;

import java.util.function.Supplier;

public class CurrentTenantIdResolver implements Supplier<String> {

    @Override
    public String get() {
        return TenantContext.requireCurrentTenantId();
    }
}
