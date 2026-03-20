package com.wfp.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ProcessStartedEvent.class, name = EventConstants.PROCESS_STARTED),
    @JsonSubTypes.Type(value = ProcessCompletedEvent.class, name = EventConstants.PROCESS_COMPLETED),
    @JsonSubTypes.Type(value = ProcessCancelledEvent.class, name = EventConstants.PROCESS_CANCELLED),
    @JsonSubTypes.Type(value = TaskCreatedEvent.class, name = EventConstants.TASK_CREATED),
    @JsonSubTypes.Type(value = TaskAssignedEvent.class, name = EventConstants.TASK_ASSIGNED),
    @JsonSubTypes.Type(value = TaskCompletedEvent.class, name = EventConstants.TASK_COMPLETED),
    @JsonSubTypes.Type(value = TaskDelegatedEvent.class, name = EventConstants.TASK_DELEGATED),
})
public abstract class BaseEvent {

    private String eventId;
    private String eventType;
    private String tenantId;
    private String userId;
    private Instant timestamp;

    public void initDefaults(String eventType, String tenantId, String userId) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.tenantId = tenantId;
        this.userId = userId;
        this.timestamp = Instant.now();
    }
}
