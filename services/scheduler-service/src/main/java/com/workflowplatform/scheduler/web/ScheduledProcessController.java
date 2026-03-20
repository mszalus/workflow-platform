package com.workflowplatform.scheduler.web;

import com.workflowplatform.scheduler.dto.ScheduleExecutionDto;
import com.workflowplatform.scheduler.dto.ScheduledProcessDto;
import com.workflowplatform.scheduler.dto.ScheduledProcessRequest;
import com.workflowplatform.scheduler.service.SchedulerManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/scheduled-processes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('WORKFLOW_ADMIN') or hasRole('SCHEDULER_MANAGER')")
public class ScheduledProcessController {

    private final SchedulerManagementService schedulerManagementService;

    /**
     * POST /api/v1/scheduled-processes
     * Create and register a new scheduled process.
     */
    @PostMapping
    public ResponseEntity<ScheduledProcessDto> createScheduledProcess(
            @Valid @RequestBody ScheduledProcessRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        ScheduledProcessDto dto = schedulerManagementService.scheduleProcess(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * GET /api/v1/scheduled-processes
     * List all scheduled processes for the tenant, with optional enabled filter.
     */
    @GetMapping
    public ResponseEntity<Page<ScheduledProcessDto>> listScheduledProcesses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean enabled,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(schedulerManagementService.listSchedules(tenantId, enabled, pageRequest));
    }

    /**
     * GET /api/v1/scheduled-processes/{id}
     * Get a single scheduled process by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScheduledProcessDto> getScheduledProcess(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        return ResponseEntity.ok(schedulerManagementService.getSchedule(id, tenantId));
    }

    /**
     * PUT /api/v1/scheduled-processes/{id}
     * Update an existing scheduled process.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScheduledProcessDto> updateScheduledProcess(
            @PathVariable UUID id,
            @Valid @RequestBody ScheduledProcessRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        return ResponseEntity.ok(schedulerManagementService.updateSchedule(id, tenantId, request));
    }

    /**
     * DELETE /api/v1/scheduled-processes/{id}
     * Delete a scheduled process and remove its Quartz job.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScheduledProcess(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        schedulerManagementService.deleteSchedule(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/scheduled-processes/{id}/trigger
     * Manually trigger a scheduled process immediately.
     */
    @PostMapping("/{id}/trigger")
    public ResponseEntity<Void> triggerNow(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        schedulerManagementService.manualTrigger(id, tenantId);
        return ResponseEntity.accepted().build();
    }

    /**
     * PUT /api/v1/scheduled-processes/{id}/enable
     * Enable a scheduled process and resume its Quartz trigger.
     */
    @PutMapping("/{id}/enable")
    public ResponseEntity<Void> enableScheduledProcess(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        schedulerManagementService.enableDisable(id, tenantId, true);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/v1/scheduled-processes/{id}/disable
     * Disable a scheduled process and pause its Quartz trigger.
     */
    @PutMapping("/{id}/disable")
    public ResponseEntity<Void> disableScheduledProcess(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        schedulerManagementService.enableDisable(id, tenantId, false);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/scheduled-processes/{id}/executions
     * Get paged execution history for a scheduled process.
     */
    @GetMapping("/{id}/executions")
    public ResponseEntity<Page<ScheduleExecutionDto>> getExecutions(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("firedAt").descending());
        return ResponseEntity.ok(schedulerManagementService.getExecutions(id, tenantId, pageRequest));
    }
}
