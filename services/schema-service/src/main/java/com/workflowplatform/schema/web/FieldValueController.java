package com.workflowplatform.schema.web;

import com.workflowplatform.schema.dto.FieldValueDto;
import com.workflowplatform.schema.dto.SaveFieldValuesRequest;
import com.workflowplatform.schema.service.FieldValueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/field-values")
@RequiredArgsConstructor
public class FieldValueController {

    private final FieldValueService fieldValueService;

    @GetMapping
    public ResponseEntity<List<FieldValueDto>> getFieldValues(
            @RequestParam String processInstanceId,
            @RequestParam(required = false) String taskId,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(fieldValueService.getValues(tenantId, processInstanceId, taskId));
    }

    @PostMapping
    public ResponseEntity<List<FieldValueDto>> saveFieldValues(
            @Valid @RequestBody SaveFieldValuesRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        List<FieldValueDto> saved = fieldValueService.saveValues(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteFieldValues(
            @RequestParam String processInstanceId,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        fieldValueService.deleteValues(tenantId, processInstanceId);
        return ResponseEntity.noContent().build();
    }

    private String extractTenantId(Jwt jwt) {
        return jwt.getClaimAsString("tenant_id");
    }
}
