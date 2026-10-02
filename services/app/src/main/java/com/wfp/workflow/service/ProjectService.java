package com.wfp.workflow.service;

import com.wfp.common.exception.BadRequestException;
import com.wfp.security.context.TenantContext;
import com.wfp.workflow.dto.ProjectDto;
import com.wfp.workflow.engine.WorkflowEngine;
import com.wfp.workflow.entity.ItemType;
import com.wfp.workflow.entity.Project;
import com.wfp.workflow.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final String KEY_PATTERN = "[A-Z][A-Z0-9]{1,9}";

    private final ProjectRepository projectRepository;
    private final WorkflowEngine engine;
    private final AuditService auditService;

    @Transactional
    public ProjectDto create(ProjectDto request, String actor) {
        String tenantId = TenantContext.requireCurrentTenantId();
        if (!request.getKey().matches(KEY_PATTERN)) {
            throw new BadRequestException(
                    "Project key must be 2 to 10 capital letters or digits, starting with a letter");
        }
        if (projectRepository.existsByKey(request.getKey())) {
            throw new BadRequestException("Project " + request.getKey() + " already exists");
        }
        if (request.getItemTypes().stream().map(ProjectDto.ItemTypeDto::getName).distinct().count()
                < request.getItemTypes().size()) {
            throw new BadRequestException("Item type names must be unique within a project");
        }
        request.getItemTypes().forEach(type ->
                engine.describe(engine.latestVersion(tenantId, type.getWorkflowKey())));

        Project project = Project.builder().tenantId(tenantId).key(request.getKey()).name(request.getName()).build();
        request.getItemTypes().forEach(type -> project.getItemTypes().add(ItemType.builder()
                .project(project).name(type.getName()).workflowKey(type.getWorkflowKey()).build()));
        projectRepository.save(project);
        auditService.record("project.created", "PROJECT", project.getKey(), actor,
                Map.of("name", project.getName()));
        return toDto(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> list() {
        return projectRepository.findAllByOrderByKeyAsc().stream().map(this::toDto).toList();
    }

    private ProjectDto toDto(Project project) {
        return ProjectDto.builder()
                .key(project.getKey())
                .name(project.getName())
                .itemTypes(project.getItemTypes().stream()
                        .map(type -> new ProjectDto.ItemTypeDto(type.getName(), type.getWorkflowKey()))
                        .toList())
                .build();
    }
}
