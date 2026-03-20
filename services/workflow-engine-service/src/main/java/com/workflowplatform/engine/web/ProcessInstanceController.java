package com.workflowplatform.engine.web;

import com.workflowplatform.engine.dto.PagedResponse;
import com.workflowplatform.engine.dto.ProcessInstanceDto;
import com.workflowplatform.engine.dto.StartProcessRequest;
import com.workflowplatform.engine.service.WorkflowEventPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/process-instances")
@RequiredArgsConstructor

public class ProcessInstanceController {

    private final RuntimeService runtimeService;
    private final WorkflowEventPublisher eventPublisher;

    @PostMapping
    
    public ResponseEntity<ProcessInstanceDto> startInstance(
            @Valid @RequestBody StartProcessRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        String actorId = jwt.getSubject();

        ProcessInstance instance;
        if (request.getProcessDefinitionId() != null) {
            instance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(request.getProcessDefinitionId())
                .businessKey(request.getBusinessKey())
                .name(request.getName())
                .tenantId(tenantId)
                .variables(request.getVariables() != null ? request.getVariables() : Collections.emptyMap())
                .start();
        } else {
            instance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey(request.getProcessDefinitionKey())
                .businessKey(request.getBusinessKey())
                .name(request.getName())
                .tenantId(tenantId)
                .variables(request.getVariables() != null ? request.getVariables() : Collections.emptyMap())
                .start();
        }

        eventPublisher.publishProcessInstanceStarted(
            instance.getId(),
            instance.getProcessDefinitionId(),
            instance.getProcessDefinitionKey(),
            instance.getBusinessKey(),
            tenantId,
            actorId
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(instance));
    }

    @GetMapping
    
    public ResponseEntity<PagedResponse<ProcessInstanceDto>> listInstances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String processDefinitionKey,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String businessKey,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");

        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
            .processInstanceTenantId(tenantId)
            .orderByStartTime().desc();

        if (processDefinitionKey != null) {
            query.processDefinitionKey(processDefinitionKey);
        }
        if (businessKey != null) {
            query.processInstanceBusinessKey(businessKey);
        }
        if ("suspended".equalsIgnoreCase(status)) {
            query.suspended();
        } else if ("active".equalsIgnoreCase(status)) {
            query.active();
        }

        long total = query.count();
        List<ProcessInstance> instances = query.listPage(page * size, size);

        List<ProcessInstanceDto> dtos = instances.stream().map(this::toDto).toList();

        return ResponseEntity.ok(PagedResponse.of(dtos, total, page, size));
    }

    @GetMapping("/{id}")
    
    public ResponseEntity<ProcessInstanceDto> getInstance(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");

        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
            .processInstanceId(id)
            .processInstanceTenantId(tenantId)
            .singleResult();

        if (instance == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toDto(instance));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelInstance(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "Cancelled by user") String reason,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        String actorId = jwt.getSubject();

        validateInstanceBelongsToTenant(id, tenantId);

        runtimeService.deleteProcessInstance(id, reason);
        eventPublisher.publishProcessInstanceCancelled(id, tenantId, actorId);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/suspend")
    
    public ResponseEntity<Void> suspendInstance(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        String actorId = jwt.getSubject();

        validateInstanceBelongsToTenant(id, tenantId);

        runtimeService.suspendProcessInstanceById(id);
        eventPublisher.publishProcessInstanceSuspended(id, tenantId, actorId);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    
    public ResponseEntity<Void> activateInstance(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        String actorId = jwt.getSubject();

        validateInstanceBelongsToTenant(id, tenantId);

        runtimeService.activateProcessInstanceById(id);
        eventPublisher.publishProcessInstanceActivated(id, tenantId, actorId);

        return ResponseEntity.noContent().build();
    }

    private void validateInstanceBelongsToTenant(String instanceId, String tenantId) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
            .processInstanceId(instanceId)
            .processInstanceTenantId(tenantId)
            .singleResult();
        if (instance == null) {
            throw new jakarta.persistence.EntityNotFoundException(
                "Process instance not found: " + instanceId);
        }
    }

    private ProcessInstanceDto toDto(ProcessInstance pi) {
        return ProcessInstanceDto.builder()
            .id(pi.getId())
            .processDefinitionId(pi.getProcessDefinitionId())
            .processDefinitionKey(pi.getProcessDefinitionKey())
            .processDefinitionName(pi.getProcessDefinitionName())
            .businessKey(pi.getBusinessKey())
            .name(pi.getName())
            .suspended(pi.isSuspended())
            .ended(pi.isEnded())
            .tenantId(pi.getTenantId())
            .startTime(pi.getStartTime() != null ? pi.getStartTime().toInstant() : null)
            .status(pi.isSuspended() ? "SUSPENDED" : pi.isEnded() ? "ENDED" : "ACTIVE")
            .build();
    }
}
