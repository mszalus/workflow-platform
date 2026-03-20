package com.workflowplatform.scheduler.repository;

import com.workflowplatform.scheduler.domain.ScheduleExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ScheduleExecutionRepository extends JpaRepository<ScheduleExecution, UUID> {

    Page<ScheduleExecution> findByScheduledProcessIdOrderByFiredAtDesc(
            UUID scheduledProcessId, Pageable pageable);

    Page<ScheduleExecution> findByScheduledProcessIdAndScheduledProcessTenantId(
            UUID scheduledProcessId, String tenantId, Pageable pageable);
}
