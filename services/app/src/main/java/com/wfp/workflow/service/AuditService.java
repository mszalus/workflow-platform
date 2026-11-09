package com.wfp.workflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfp.common.dto.PagedResponse;
import com.wfp.security.context.TenantContext;
import com.wfp.workflow.dto.AuditEntryDto;
import com.wfp.workflow.entity.AuditEntry;
import com.wfp.workflow.repository.AuditEntryRepository;
import com.wfp.workflow.repository.AuditEntrySpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEntryRepository auditEntryRepository;
    private final ObjectMapper objectMapper;

    public void record(String eventType, String entityType, String entityId, String userId,
                       Map<String, Object> details) {
        auditEntryRepository.save(AuditEntry.builder()
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .userId(userId)
                .tenantId(TenantContext.requireCurrentTenantId())
                .timestamp(Instant.now())
                .details(serialize(details))
                .sourceService("app")
                .build());
    }

    public PagedResponse<AuditEntryDto> query(String entityType, String entityId, String userId,
                                                String eventType, Instant from, Instant to,
                                                int page, int size) {
        String tenantId = TenantContext.requireCurrentTenantId();

        Specification<AuditEntry> spec = Specification.where(AuditEntrySpecification.withTenant(tenantId))
                .and(AuditEntrySpecification.withEntityType(entityType))
                .and(AuditEntrySpecification.withEntityId(entityId))
                .and(AuditEntrySpecification.withUserId(userId))
                .and(AuditEntrySpecification.withEventType(eventType))
                .and(AuditEntrySpecification.afterTimestamp(from))
                .and(AuditEntrySpecification.beforeTimestamp(to));

        Page<AuditEntry> p = auditEntryRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")));

        List<AuditEntryDto> items = p.getContent().stream().map(this::toDto).toList();
        return PagedResponse.of(items, page, size, p.getTotalElements());
    }

    private AuditEntryDto toDto(AuditEntry e) {
        Map<String, Object> detailsMap = null;
        if (e.getDetails() != null) {
            try {
                detailsMap = objectMapper.readValue(e.getDetails(), new TypeReference<>() {});
            } catch (Exception ex) {
                log.warn("Failed to parse audit details JSON", ex);
            }
        }
        return AuditEntryDto.builder()
                .id(e.getId().toString())
                .eventType(e.getEventType())
                .entityType(e.getEntityType())
                .entityId(e.getEntityId())
                .userId(e.getUserId())
                .tenantId(e.getTenantId())
                .timestamp(e.getTimestamp())
                .details(detailsMap)
                .sourceService(e.getSourceService())
                .build();
    }

    private String serialize(Object details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            log.error("Failed to serialize audit event", e);
            return "{}";
        }
    }
}
