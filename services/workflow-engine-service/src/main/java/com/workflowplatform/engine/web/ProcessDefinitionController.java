package com.workflowplatform.engine.web;

import com.workflowplatform.engine.dto.ErrorResponse;
import com.workflowplatform.engine.dto.PagedResponse;
import com.workflowplatform.engine.dto.ProcessDefinitionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/process-definitions")
@RequiredArgsConstructor

public class ProcessDefinitionController {

    private final RepositoryService repositoryService;

    @GetMapping
    
    public ResponseEntity<PagedResponse<ProcessDefinitionDto>> listProcessDefinitions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String key,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean latestVersion,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");

        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery()
            .processDefinitionTenantId(tenantId)
            .orderByProcessDefinitionVersion().desc();

        if (key != null && !key.isBlank()) {
            query.processDefinitionKey(key);
        }
        if (category != null && !category.isBlank()) {
            query.processDefinitionCategory(category);
        }
        if (Boolean.TRUE.equals(latestVersion)) {
            query.latestVersion();
        }

        long total = query.count();
        List<ProcessDefinition> definitions = query
            .listPage(page * size, size);

        List<ProcessDefinitionDto> dtos = definitions.stream()
            .map(pd -> toDto(pd))
            .toList();

        return ResponseEntity.ok(PagedResponse.of(dtos, total, page, size));
    }

    @GetMapping("/{id}")
    
    public ResponseEntity<ProcessDefinitionDto> getProcessDefinition(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");

        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
            .processDefinitionId(id)
            .processDefinitionTenantId(tenantId)
            .singleResult();

        if (pd == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toDto(pd));
    }

    @GetMapping("/{id}/bpmn")
    
    public ResponseEntity<byte[]> getProcessDefinitionBpmn(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");

        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
            .processDefinitionId(id)
            .processDefinitionTenantId(tenantId)
            .singleResult();

        if (pd == null) {
            return ResponseEntity.notFound().build();
        }

        try (InputStream bpmnStream = repositoryService.getProcessModel(id)) {
            byte[] bytes = bpmnStream.readAllBytes();
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + pd.getResourceName() + "\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(bytes);
        } catch (Exception e) {
            log.error("Failed to retrieve BPMN for processDefinitionId={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private ProcessDefinitionDto toDto(ProcessDefinition pd) {
        return ProcessDefinitionDto.builder()
            .id(pd.getId())
            .key(pd.getKey())
            .name(pd.getName())
            .description(pd.getDescription())
            .version(pd.getVersion())
            .category(pd.getCategory())
            .deploymentId(pd.getDeploymentId())
            .resourceName(pd.getResourceName())
            .diagramResourceName(pd.getDiagramResourceName())
            .suspended(pd.isSuspended())
            .tenantId(pd.getTenantId())
            .build();
    }
}
