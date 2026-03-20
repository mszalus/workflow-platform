package com.workflowplatform.scheduler.repository;

import com.workflowplatform.scheduler.domain.ScheduledProcess;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduledProcessRepository extends JpaRepository<ScheduledProcess, UUID> {

    Page<ScheduledProcess> findByTenantId(String tenantId, Pageable pageable);

    Page<ScheduledProcess> findByTenantIdAndEnabled(String tenantId, boolean enabled, Pageable pageable);

    Optional<ScheduledProcess> findByTenantIdAndId(String tenantId, UUID id);

    boolean existsByTenantIdAndName(String tenantId, String name);
}
