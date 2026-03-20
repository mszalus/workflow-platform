package com.workflowplatform.schema.web;

import com.workflowplatform.schema.dto.CreateSchemaRequest;
import com.workflowplatform.schema.dto.FieldSchemaDto;
import com.workflowplatform.schema.service.FieldSchemaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/schemas")
@RequiredArgsConstructor
public class FieldSchemaController {

    private final FieldSchemaService schemaService;

    @GetMapping
    public ResponseEntity<Page<FieldSchemaDto>> listSchemas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String processDefKey,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(schemaService.listSchemas(tenantId, processDefKey, pageRequest));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FieldSchemaDto> getSchema(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(schemaService.getSchema(tenantId, id));
    }

    @GetMapping("/active/{processDefKey}")
    public ResponseEntity<FieldSchemaDto> getActiveSchema(
            @PathVariable String processDefKey,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(schemaService.getActiveSchema(tenantId, processDefKey));
    }

    @PostMapping
    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    public ResponseEntity<FieldSchemaDto> createSchema(
            @Valid @RequestBody CreateSchemaRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        FieldSchemaDto created = schemaService.createSchema(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    public ResponseEntity<FieldSchemaDto> updateSchema(
            @PathVariable UUID id,
            @Valid @RequestBody CreateSchemaRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(schemaService.updateSchema(tenantId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    public ResponseEntity<Void> deleteSchema(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        schemaService.deleteSchema(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    private String extractTenantId(Jwt jwt) {
        return jwt.getClaimAsString("tenant_id");
    }
}
