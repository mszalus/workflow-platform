package com.workflowplatform.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleExecutionDto {

    private UUID id;
    private UUID scheduledProcessId;
    private Instant firedAt;
    private String status;
    private String processInstanceId;
    private String errorMessage;
}
