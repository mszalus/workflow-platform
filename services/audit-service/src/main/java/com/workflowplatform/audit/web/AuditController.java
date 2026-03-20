package com.workflowplatform.audit.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflowplatform.audit.dto.AuditEventDto;
import com.workflowplatform.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/audit-events")
@RequiredArgsConstructor
public class AuditController {

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * GET /api/v1/audit-events?eventType=&actorId=&from=&to=&page=0&size=50
     *
     * Query audit events. Restricted to users with AUDIT_VIEWER or WORKFLOW_ADMIN role.
     * tenantId is always derived from the caller's JWT.
     */
    @GetMapping
    @PreAuthorize("hasRole('AUDIT_VIEWER') or hasRole('WORKFLOW_ADMIN')")
    public ResponseEntity<Page<AuditEventDto>> listAuditEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");

        // Validate page size to prevent abuse
        int safeSize = Math.min(size, 200);
        PageRequest pageRequest = PageRequest.of(page, safeSize);

        Page<AuditEventDto> results = auditEventRepository
                .findWithFilters(tenantId, eventType, actorId, from, to, pageRequest)
                .map(entity -> AuditEventDto.from(entity, objectMapper));

        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/v1/audit-events/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('AUDIT_VIEWER') or hasRole('WORKFLOW_ADMIN')")
    public ResponseEntity<AuditEventDto> getAuditEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");

        return auditEventRepository.findById(id)
                .filter(e -> tenantId.equals(e.getTenantId()))
                .map(entity -> ResponseEntity.ok(AuditEventDto.from(entity, objectMapper)))
                .orElse(ResponseEntity.notFound().build());
    }
}
