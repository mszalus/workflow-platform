package com.wfp.workflow.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class ProcessInstanceDto {
    private String id;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String processDefinitionName;
    private String businessKey;
    private Instant startTime;
    private Instant endTime;
    private String startUserId;
    private String tenantId;
    private Map<String, Object> variables;
}
