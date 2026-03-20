package com.wfp.security.filter;

import com.wfp.security.context.TenantContext;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantHibernateFilter {

    public static final String FILTER_NAME = "tenantFilter";
    public static final String PARAMETER_NAME = "tenantId";

    private final EntityManager entityManager;

    public void enableFilter() {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter(FILTER_NAME).setParameter(PARAMETER_NAME, tenantId);
            log.debug("Enabled tenant Hibernate filter for tenant: {}", tenantId);
        }
    }

    public void disableFilter() {
        Session session = entityManager.unwrap(Session.class);
        session.disableFilter(FILTER_NAME);
    }
}
