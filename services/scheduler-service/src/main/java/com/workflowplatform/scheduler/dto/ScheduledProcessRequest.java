package com.workflowplatform.scheduler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledProcessRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotBlank(message = "Process definition key is required")
    private String processDefKey;

    @NotBlank(message = "Cron expression is required")
    @Pattern(
        regexp = "^[0-9*/?LW,#\\- ]{9,}$",
        message = "Must be a valid cron expression (6 or 7 fields)"
    )
    private String cronExpression;

    /**
     * Optional process variables as a map; serialized to JSON for storage.
     */
    private Map<String, Object> variables;

    @Builder.Default
    private boolean enabled = true;
}
