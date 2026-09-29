package com.wfp.security.filter;

import com.wfp.security.context.TenantContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TenantHibernateFilterTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Session session = mock(Session.class);
    private final TenantHibernateFilter tenantHibernateFilter = new TenantHibernateFilter(entityManager);

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void enablesFilterForCurrentTenant() {
        Filter filter = mock(Filter.class);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter(TenantHibernateFilter.FILTER_NAME)).thenReturn(filter);
        TenantContext.setCurrentTenantId("tenant-a");

        tenantHibernateFilter.enableFilter();

        verify(filter).setParameter(TenantHibernateFilter.PARAMETER_NAME, "tenant-a");
    }

    @Test
    void failsClosedWithoutTenant() {
        assertThatThrownBy(tenantHibernateFilter::enableFilter).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(entityManager);
    }
}
