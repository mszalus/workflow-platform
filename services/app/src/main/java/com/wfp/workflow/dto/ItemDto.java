package com.wfp.workflow.dto;

import com.wfp.workflow.engine.StatusCategory;
import com.wfp.workflow.engine.Transition;
import com.wfp.workflow.entity.Priority;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ItemDto {
    private String key;
    private String project;
    private String type;
    private String title;
    private String description;
    private Priority priority;
    private String assignee;
    private String reporter;
    private String statusId;
    private String statusName;
    private StatusCategory statusCategory;
    private Map<String, Object> fields;
    private Instant createdAt;
    private Instant updatedAt;
    private List<Transition> transitions;
}
