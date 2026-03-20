package com.workflowplatform.audit.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflowplatform.audit.domain.AuditEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventDto {

    private UUID id;
    private String eventId;
    private String eventType;
    private String tenantId;
    private String actorId;
    private Instant occurredAt;
    private Instant receivedAt;
    private String topic;

    /**
     * Payload deserialized from the stored JSONB string into a Map for client consumption.
     */
    private Map<String, Object> payload;

    /**
     * Map a domain entity to a DTO. Uses the provided ObjectMapper to deserialize payload.
     */
    public static AuditEventDto from(AuditEvent entity, ObjectMapper objectMapper) {
        Map<String, Object> payloadMap = null;
        if (entity.getPayload() != null && !entity.getPayload().isBlank()) {
            try {
                payloadMap = objectMapper.readValue(entity.getPayload(),
                        new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {
                // Return null payload rather than failing the request
            }
        }

        return AuditEventDto.builder()
                .id(entity.getId())
                .eventId(entity.getEventId())
                .eventType(entity.getEventType())
                .tenantId(entity.getTenantId())
                .actorId(entity.getActorId())
                .occurredAt(entity.getOccurredAt())
                .receivedAt(entity.getReceivedAt())
                .topic(entity.getTopic())
                .payload(payloadMap)
                .build();
    }
}
