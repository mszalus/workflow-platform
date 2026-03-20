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
public class ProcessStartedEvent extends BaseEvent {

    private String processInstanceId;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String processName;
    private String businessKey;
    private Map<String, Object> variables;
}
