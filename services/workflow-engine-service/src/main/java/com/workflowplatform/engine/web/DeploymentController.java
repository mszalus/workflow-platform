package com.workflowplatform.engine.web;

import com.workflowplatform.engine.dto.DeploymentDto;
import com.workflowplatform.engine.dto.PagedResponse;
import com.workflowplatform.engine.dto.ProcessDefinitionDto;
import com.workflowplatform.engine.service.WorkflowEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.DeploymentQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/deployments")
@RequiredArgsConstructor

public class DeploymentController {

    private final RepositoryService repositoryService;
    private final WorkflowEventPublisher eventPublisher;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('WORKFLOW_ADMIN') or hasRole('DEPLOYER')")
    
    public ResponseEntity<DeploymentDto> deploy(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) String deploymentName,
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal Jwt jwt) throws IOException {

        String tenantId = jwt.getClaimAsString("tenant_id");
        String actorId = jwt.getSubject();

        DeploymentBuilder builder = repositoryService.createDeployment()
            .tenantId(tenantId);

        if (deploymentName != null && !deploymentName.isBlank()) {
            builder.name(deploymentName);
        }
        if (category != null && !category.isBlank()) {
            builder.category(category);
        }

        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();
            if (filename == null) {
                filename = file.getName();
            }
            builder.addInputStream(filename, file.getInputStream());
        }

        Deployment deployment = builder.deploy();

        List<ProcessDefinition> processDefinitions = repositoryService
            .createProcessDefinitionQuery()
            .deploymentId(deployment.getId())
            .list();

        eventPublisher.publishDeploymentCreated(
            deployment.getId(),
            deployment.getName(),
            tenantId,
            actorId
        );

        DeploymentDto dto = toDto(deployment, processDefinitions);

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    
    public ResponseEntity<PagedResponse<DeploymentDto>> listDeployments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");

        DeploymentQuery query = repositoryService.createDeploymentQuery()
            .deploymentTenantId(tenantId)
            .orderByDeploymentTime().desc();

        if (name != null && !name.isBlank()) {
            query.deploymentNameLike("%" + name + "%");
        }

        long total = query.count();
        List<Deployment> deployments = query.listPage(page * size, size);

        List<DeploymentDto> dtos = deployments.stream()
            .map(d -> toDto(d, List.of()))
            .toList();

        return ResponseEntity.ok(PagedResponse.of(dtos, total, page, size));
    }

    @GetMapping("/{id}")
    
    public ResponseEntity<DeploymentDto> getDeployment(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");

        Deployment deployment = repositoryService.createDeploymentQuery()
            .deploymentId(id)
            .deploymentTenantId(tenantId)
            .singleResult();

        if (deployment == null) {
            return ResponseEntity.notFound().build();
        }

        List<ProcessDefinition> processDefinitions = repositoryService
            .createProcessDefinitionQuery()
            .deploymentId(id)
            .list();

        return ResponseEntity.ok(toDto(deployment, processDefinitions));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    public ResponseEntity<Void> deleteDeployment(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean cascade,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");

        Deployment deployment = repositoryService.createDeploymentQuery()
            .deploymentId(id)
            .deploymentTenantId(tenantId)
            .singleResult();

        if (deployment == null) {
            return ResponseEntity.notFound().build();
        }

        repositoryService.deleteDeployment(id, cascade);

        return ResponseEntity.noContent().build();
    }

    private DeploymentDto toDto(Deployment deployment, List<ProcessDefinition> processDefinitions) {
        List<ProcessDefinitionDto> pdDtos = processDefinitions.stream()
            .map(pd -> ProcessDefinitionDto.builder()
                .id(pd.getId())
                .key(pd.getKey())
                .name(pd.getName())
                .description(pd.getDescription())
                .version(pd.getVersion())
                .category(pd.getCategory())
                .deploymentId(pd.getDeploymentId())
                .resourceName(pd.getResourceName())
                .suspended(pd.isSuspended())
                .tenantId(pd.getTenantId())
                .build())
            .toList();

        return DeploymentDto.builder()
            .id(deployment.getId())
            .name(deployment.getName())
            .category(deployment.getCategory())
            .tenantId(deployment.getTenantId())
            .deployedAt(deployment.getDeploymentTime() != null
                ? deployment.getDeploymentTime().toInstant()
                : null)
            .processDefinitions(pdDtos)
            .build();
    }
}
