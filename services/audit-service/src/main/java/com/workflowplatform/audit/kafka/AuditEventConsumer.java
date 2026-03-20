package com.workflowplatform.audit.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflowplatform.audit.domain.AuditEvent;
import com.workflowplatform.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "${kafka.topics.workflow-events:workflow.events}",
        groupId = "${spring.kafka.consumer.group-id:audit-service}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeWorkflowEvents(ConsumerRecord<String, Map<String, Object>> record) {
        persistAuditEvent(record, "workflow.events");
    }

    @KafkaListener(
        topics = "${kafka.topics.schema-events:schema.events}",
        groupId = "${spring.kafka.consumer.group-id:audit-service}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeSchemaEvents(ConsumerRecord<String, Map<String, Object>> record) {
        persistAuditEvent(record, "schema.events");
    }

    private void persistAuditEvent(ConsumerRecord<String, Map<String, Object>> record, String topic) {
        Map<String, Object> eventMap = record.value();
        if (eventMap == null) {
            log.warn("Received null event from topic={} partition={} offset={}",
                topic, record.partition(), record.offset());
            return;
        }

        // Extract eventId; fall back to a generated UUID for events without one
        String eventId = (String) eventMap.get("eventId");
        if (eventId == null || eventId.isBlank()) {
            eventId = UUID.randomUUID().toString();
            log.warn("Event from topic={} missing eventId, generated fallback={}", topic, eventId);
        }

        // Idempotency: skip if already persisted
        if (auditEventRepository.existsByEventId(eventId)) {
            log.debug("Skipping duplicate audit event id={} topic={}", eventId, topic);
            return;
        }

        String eventType = (String) eventMap.getOrDefault("eventType", "UNKNOWN");
        String tenantId  = (String) eventMap.get("tenantId");
        String actorId   = (String) eventMap.get("actorId");

        Instant occurredAt;
        Object occurredAtRaw = eventMap.get("occurredAt");
        if (occurredAtRaw instanceof String s) {
            try {
                occurredAt = Instant.parse(s);
            } catch (Exception e) {
                occurredAt = Instant.now();
            }
        } else {
            occurredAt = Instant.now();
        }

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(eventMap);
        } catch (Exception e) {
            log.error("Failed to serialize event payload for eventId={}", eventId, e);
            payloadJson = "{}";
        }

        AuditEvent auditEvent = AuditEvent.builder()
            .eventId(eventId)
            .eventType(eventType)
            .tenantId(tenantId)
            .actorId(actorId)
            .occurredAt(occurredAt)
            .receivedAt(Instant.now())
            .payload(payloadJson)
            .topic(topic)
            .build();

        auditEventRepository.save(auditEvent);

        log.debug("Persisted audit event id={} type={} tenant={}", eventId, eventType, tenantId);
    }
}
