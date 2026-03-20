package com.workflowplatform.scheduler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflowplatform.scheduler.domain.ScheduleExecution;
import com.workflowplatform.scheduler.domain.ScheduledProcess;
import com.workflowplatform.scheduler.dto.ScheduleExecutionDto;
import com.workflowplatform.scheduler.dto.ScheduledProcessDto;
import com.workflowplatform.scheduler.dto.ScheduledProcessRequest;
import com.workflowplatform.scheduler.quartz.ProcessTriggerJob;
import com.workflowplatform.scheduler.repository.ScheduleExecutionRepository;
import com.workflowplatform.scheduler.repository.ScheduledProcessRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SchedulerManagementService {

    private final Scheduler quartzScheduler;
    private final ScheduledProcessRepository scheduledProcessRepository;
    private final ScheduleExecutionRepository scheduleExecutionRepository;
    private final ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public ScheduledProcessDto scheduleProcess(String tenantId, ScheduledProcessRequest request) {
        String variablesJson = serializeVariables(request.getVariables());

        ScheduledProcess sp = ScheduledProcess.builder()
            .tenantId(tenantId)
            .name(request.getName())
            .description(request.getDescription())
            .processDefKey(request.getProcessDefKey())
            .cronExpression(request.getCronExpression())
            .variables(variablesJson)
            .enabled(request.isEnabled())
            .build();

        sp = scheduledProcessRepository.save(sp);

        if (sp.isEnabled()) {
            try {
                registerQuartzJob(sp);
            } catch (SchedulerException e) {
                log.error("Failed to register Quartz job for scheduledProcessId={}", sp.getId(), e);
                throw new RuntimeException("Failed to register cron trigger: " + e.getMessage(), e);
            }
        }

        return toDto(sp);
    }

    public ScheduledProcessDto updateSchedule(UUID id, String tenantId, ScheduledProcessRequest request) {
        ScheduledProcess sp = findOrThrow(id, tenantId);

        sp.setName(request.getName());
        sp.setDescription(request.getDescription());
        sp.setProcessDefKey(request.getProcessDefKey());
        sp.setCronExpression(request.getCronExpression());
        sp.setVariables(serializeVariables(request.getVariables()));
        sp.setEnabled(request.isEnabled());
        sp = scheduledProcessRepository.save(sp);

        try {
            if (sp.isEnabled()) {
                registerQuartzJob(sp);
            } else {
                removeQuartzJob(sp);
            }
        } catch (SchedulerException e) {
            log.error("Failed to update Quartz job for scheduledProcessId={}", id, e);
            throw new RuntimeException("Failed to update cron trigger: " + e.getMessage(), e);
        }

        return toDto(sp);
    }

    public void deleteSchedule(UUID id, String tenantId) {
        ScheduledProcess sp = findOrThrow(id, tenantId);

        try {
            removeQuartzJob(sp);
        } catch (SchedulerException e) {
            log.warn("Failed to remove Quartz job during delete for scheduledProcessId={}, continuing: {}",
                id, e.getMessage());
        }

        scheduledProcessRepository.delete(sp);
        log.info("Deleted scheduledProcess id={} tenant={}", id, tenantId);
    }

    public void enableDisable(UUID id, String tenantId, boolean enabled) {
        ScheduledProcess sp = findOrThrow(id, tenantId);
        sp.setEnabled(enabled);
        sp = scheduledProcessRepository.save(sp);

        try {
            if (enabled) {
                registerQuartzJob(sp);
                log.info("Enabled schedule id={} tenant={}", id, tenantId);
            } else {
                String group = sanitizeGroup(tenantId);
                TriggerKey triggerKey = TriggerKey.triggerKey(id.toString(), group);
                if (quartzScheduler.checkExists(triggerKey)) {
                    quartzScheduler.pauseTrigger(triggerKey);
                }
                log.info("Paused schedule id={} tenant={}", id, tenantId);
            }
        } catch (SchedulerException e) {
            log.error("Failed to toggle enabled={} for scheduledProcessId={}", enabled, id, e);
            throw new RuntimeException("Failed to update trigger state: " + e.getMessage(), e);
        }
    }

    public void manualTrigger(UUID id, String tenantId) {
        ScheduledProcess sp = findOrThrow(id, tenantId);
        String group = sanitizeGroup(tenantId);
        JobKey jobKey = JobKey.jobKey(id.toString(), group);

        try {
            if (!quartzScheduler.checkExists(jobKey)) {
                // Register the job ad-hoc so we can trigger it immediately
                registerQuartzJob(sp);
            }
            quartzScheduler.triggerJob(jobKey);
            log.info("Manually triggered scheduledProcessId={} tenant={}", id, tenantId);
        } catch (SchedulerException e) {
            log.error("Failed to manually trigger scheduledProcessId={}", id, e);
            throw new RuntimeException("Failed to trigger job: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<ScheduledProcessDto> listSchedules(String tenantId, Boolean enabled, Pageable pageable) {
        Page<ScheduledProcess> page = (enabled != null)
            ? scheduledProcessRepository.findByTenantIdAndEnabled(tenantId, enabled, pageable)
            : scheduledProcessRepository.findByTenantId(tenantId, pageable);
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ScheduledProcessDto getSchedule(UUID id, String tenantId) {
        return toDto(findOrThrow(id, tenantId));
    }

    @Transactional(readOnly = true)
    public Page<ScheduleExecutionDto> getExecutions(UUID id, String tenantId, Pageable pageable) {
        // Verify ownership
        findOrThrow(id, tenantId);
        return scheduleExecutionRepository
            .findByScheduledProcessIdAndScheduledProcessTenantId(id, tenantId, pageable)
            .map(this::toExecutionDto);
    }

    // -------------------------------------------------------------------------
    // Quartz helpers
    // -------------------------------------------------------------------------

    private void registerQuartzJob(ScheduledProcess sp) throws SchedulerException {
        String jobName = sp.getId().toString();
        String group   = sanitizeGroup(sp.getTenantId());

        sp.setQuartzJobName(jobName);
        sp.setQuartzGroup(group);
        scheduledProcessRepository.save(sp);

        JobDetail jobDetail = buildJobDetail(sp, jobName, group);
        Trigger trigger     = buildCronTrigger(sp, jobName, group);

        if (quartzScheduler.checkExists(jobDetail.getKey())) {
            quartzScheduler.rescheduleJob(trigger.getKey(), trigger);
            log.info("Rescheduled Quartz job jobName={} group={} cron={}",
                jobName, group, sp.getCronExpression());
        } else {
            quartzScheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled new Quartz job jobName={} group={} cron={}",
                jobName, group, sp.getCronExpression());
        }
    }

    private void removeQuartzJob(ScheduledProcess sp) throws SchedulerException {
        String group  = sanitizeGroup(sp.getTenantId());
        JobKey jobKey = JobKey.jobKey(sp.getId().toString(), group);
        if (quartzScheduler.checkExists(jobKey)) {
            quartzScheduler.deleteJob(jobKey);
            log.info("Deleted Quartz job jobKey={}/{}", group, sp.getId());
        }
    }

    private JobDetail buildJobDetail(ScheduledProcess sp, String jobName, String group) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(ProcessTriggerJob.KEY_SCHEDULED_PROCESS_ID, sp.getId().toString());
        dataMap.put(ProcessTriggerJob.KEY_TENANT_ID, sp.getTenantId());
        dataMap.put(ProcessTriggerJob.KEY_PROCESS_DEF_KEY, sp.getProcessDefKey());
        if (sp.getVariables() != null) {
            dataMap.put(ProcessTriggerJob.KEY_VARIABLES_JSON, sp.getVariables());
        }

        return JobBuilder.newJob(ProcessTriggerJob.class)
            .withIdentity(jobName, group)
            .withDescription(sp.getName())
            .usingJobData(dataMap)
            .storeDurably(true)
            .build();
    }

    private CronTrigger buildCronTrigger(ScheduledProcess sp, String jobName, String group) {
        return TriggerBuilder.newTrigger()
            .withIdentity(jobName, group)
            .withDescription("Trigger for: " + sp.getName())
            .withSchedule(CronScheduleBuilder
                .cronSchedule(sp.getCronExpression())
                .withMisfireHandlingInstructionDoNothing())
            .build();
    }

    private String sanitizeGroup(String tenantId) {
        return "tenant-" + tenantId.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    // -------------------------------------------------------------------------
    // Mapping helpers
    // -------------------------------------------------------------------------

    private ScheduledProcess findOrThrow(UUID id, String tenantId) {
        return scheduledProcessRepository.findByTenantIdAndId(tenantId, id)
            .orElseThrow(() -> new EntityNotFoundException(
                "ScheduledProcess not found: " + id + " for tenant " + tenantId));
    }

    private ScheduledProcessDto toDto(ScheduledProcess sp) {
        Map<String, Object> vars = deserializeVariables(sp.getVariables());
        return ScheduledProcessDto.builder()
            .id(sp.getId())
            .tenantId(sp.getTenantId())
            .name(sp.getName())
            .description(sp.getDescription())
            .processDefKey(sp.getProcessDefKey())
            .cronExpression(sp.getCronExpression())
            .variables(vars)
            .enabled(sp.isEnabled())
            .quartzGroup(sp.getQuartzGroup())
            .quartzJobName(sp.getQuartzJobName())
            .createdAt(sp.getCreatedAt())
            .updatedAt(sp.getUpdatedAt())
            .lastFiredAt(sp.getLastFiredAt())
            .nextFireAt(sp.getNextFireAt())
            .build();
    }

    private ScheduleExecutionDto toExecutionDto(ScheduleExecution ex) {
        return ScheduleExecutionDto.builder()
            .id(ex.getId())
            .scheduledProcessId(ex.getScheduledProcess() != null ? ex.getScheduledProcess().getId() : null)
            .firedAt(ex.getFiredAt())
            .status(ex.getStatus())
            .processInstanceId(ex.getProcessInstanceId())
            .errorMessage(ex.getErrorMessage())
            .build();
    }

    private String serializeVariables(Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize variables: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> deserializeVariables(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize variables: {}", e.getMessage());
            return null;
        }
    }
}
