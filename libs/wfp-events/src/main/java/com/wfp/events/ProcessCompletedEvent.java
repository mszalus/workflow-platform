package com.wfp.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessCompletedEvent extends BaseEvent {

    private String processInstanceId;
    private String processDefinitionKey;
    private String processName;
    private long durationMillis;
}
