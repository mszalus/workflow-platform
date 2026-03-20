package com.workflowplatform.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceDto {

    private String userId;
    private boolean emailEnabled;
    private boolean inAppEnabled;

    /**
     * Per-type channel override map, e.g.:
     * {"TASK_ASSIGNED": {"email": false}, "PROCESS_COMPLETED": {"inApp": true}}
     */
    private Map<String, Object> preferences;
}
