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
public class TaskDelegatedEvent extends BaseEvent {

    private String taskId;
    private String taskName;
    private String processInstanceId;
    private String delegatedFrom;
    private String delegatedTo;
    private String comment;
}
