package com.wfp.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCompletedEvent extends BaseEvent {

    private String taskId;
    private String taskName;
    private String processInstanceId;
    private String processDefinitionKey;
    private String completedBy;
    private Map<String, Object> outcome;
    private long durationMillis;
}
