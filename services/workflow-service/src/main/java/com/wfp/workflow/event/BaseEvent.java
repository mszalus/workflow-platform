package com.wfp.workflow.event;

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
