package com.workflowplatform.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteTaskRequest {

    /** Variables to set on task completion */
    private Map<String, Object> variables;

    /** Optional outcome / transition key */
    private String outcome;

    /** Optional comment to attach to the task */
    private String comment;
}
