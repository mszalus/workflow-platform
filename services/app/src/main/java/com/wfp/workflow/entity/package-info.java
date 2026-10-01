@FilterDef(name = "tenantFilter", autoEnabled = true, applyToLoadByKey = true,
        parameters = @ParamDef(name = "tenantId", type = String.class, resolver = CurrentTenantIdResolver.class))
package com.wfp.workflow.entity;

import com.wfp.security.filter.CurrentTenantIdResolver;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
