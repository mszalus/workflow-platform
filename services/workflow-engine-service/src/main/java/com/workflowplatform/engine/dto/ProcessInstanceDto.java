package com.workflowplatform.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessInstanceDto {

    private String id;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String processDefinitionName;
    private String businessKey;
    private String name;
    private String status;
    private boolean suspended;
    private boolean ended;
    private String tenantId;
    private String startedBy;
    private Instant startTime;
    private Instant endTime;
    private Map<String, Object> variables;
}
