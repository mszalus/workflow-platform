package com.workflowplatform.scheduler.quartz;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflowplatform.scheduler.domain.ScheduleExecution;
import com.workflowplatform.scheduler.domain.ScheduledProcess;
import com.workflowplatform.scheduler.repository.ScheduleExecutionRepository;
import com.workflowplatform.scheduler.repository.ScheduledProcessRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Quartz job that fires at a configured cron schedule and calls the
 * workflow-engine-service REST API to start a process instance.
 *
 * JobDataMap keys:
 *   - scheduledProcessId  (UUID string)
 *   - tenantId            (String)
 *   - processDefKey       (String)
 *   - variablesJson       (String, optional JSON object)
 */
@Slf4j
@DisallowConcurrentExecution
public class ProcessTriggerJob extends QuartzJobBean {

    public static final String KEY_SCHEDULED_PROCESS_ID = "scheduledProcessId";
    public static final String KEY_TENANT_ID            = "tenantId";
    public static final String KEY_PROCESS_DEF_KEY      = "processDefKey";
    public static final String KEY_VARIABLES_JSON       = "variablesJson";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ScheduledProcessRepository scheduledProcessRepository;

    @Autowired
    private ScheduleExecutionRepository scheduleExecutionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${workflow-engine.base-url:http://localhost:8080}")
    private String workflowEngineBaseUrl;

    @Value("${workflow-engine.service-token:}")
    private String serviceToken;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();

        String scheduledProcessIdStr = dataMap.getString(KEY_SCHEDULED_PROCESS_ID);
        String tenantId              = dataMap.getString(KEY_TENANT_ID);
        String processDefKey         = dataMap.getString(KEY_PROCESS_DEF_KEY);
        String variablesJson         = dataMap.getString(KEY_VARIABLES_JSON);

        log.info("Firing ProcessTriggerJob scheduledProcessId={} processDefKey={} tenant={}",
            scheduledProcessIdStr, processDefKey, tenantId);

        UUID scheduledProcessId = scheduledProcessIdStr != null
            ? UUID.fromString(scheduledProcessIdStr) : null;

        ScheduledProcess scheduledProcess = null;
        if (scheduledProcessId != null) {
            scheduledProcess = scheduledProcessRepository.findById(scheduledProcessId).orElse(null);
        }

        ScheduleExecution execution = ScheduleExecution.builder()
            .scheduledProcess(scheduledProcess)
            .firedAt(Instant.now())
            .status("RUNNING")
            .build();

        if (scheduledProcess != null) {
            execution = scheduleExecutionRepository.save(execution);
        }

        try {
            Map<String, Object> variables = parseVariables(variablesJson);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("processDefinitionKey", processDefKey);
            requestBody.put("businessKey", "scheduled-" + Instant.now().toEpochMilli());
            requestBody.put("name", "Scheduled: " + processDefKey);
            requestBody.put("variables", variables);
            requestBody.put("tenantId", tenantId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (tenantId != null && !tenantId.isBlank()) {
                headers.set("X-Tenant-Id", tenantId);
            }
            if (serviceToken != null && !serviceToken.isBlank()) {
                headers.setBearerAuth(serviceToken);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String url = workflowEngineBaseUrl + "/api/v1/process-instances";

            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);

            String instanceId = null;
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                instanceId = (String) response.getBody().get("id");
                log.info("Started process instance id={} from scheduled trigger processDefKey={} tenant={}",
                    instanceId, processDefKey, tenantId);
            }

            // Update last fired time and execution record
            if (scheduledProcess != null) {
                scheduledProcess.setLastFiredAt(Instant.now());
                scheduledProcessRepository.save(scheduledProcess);

                execution.setStatus("SUCCESS");
                execution.setProcessInstanceId(instanceId);
                scheduleExecutionRepository.save(execution);
            }

        } catch (HttpClientErrorException e) {
            log.error("HTTP error starting process processDefKey={} tenant={}: status={} body={}",
                processDefKey, tenantId, e.getStatusCode(), e.getResponseBodyAsString());

            if (scheduledProcess != null) {
                execution.setStatus("FAILED");
                execution.setErrorMessage("HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
                scheduleExecutionRepository.save(execution);
            }
        } catch (Exception e) {
            log.error("Failed to start process processDefKey={} tenant={}", processDefKey, tenantId, e);

            if (scheduledProcess != null) {
                execution.setStatus("FAILED");
                execution.setErrorMessage(e.getMessage());
                scheduleExecutionRepository.save(execution);
            }

            throw new JobExecutionException("Failed to trigger process: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> parseVariables(String variablesJson) {
        if (variablesJson == null || variablesJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(variablesJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse variablesJson, using empty map: {}", e.getMessage());
            return Map.of();
        }
    }
}
