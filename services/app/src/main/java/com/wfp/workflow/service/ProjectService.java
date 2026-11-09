package com.wfp.workflow.service;

import com.wfp.common.exception.BadRequestException;
import com.wfp.common.exception.NotFoundException;
import com.wfp.security.context.TenantContext;
import com.wfp.workflow.dto.ProjectDto;
import com.wfp.workflow.dto.UpdateProjectRequest;
import com.wfp.workflow.engine.WorkflowEngine;
import com.wfp.workflow.entity.ItemType;
import com.wfp.workflow.entity.Project;
import com.wfp.workflow.repository.FieldSchemaRepository;
import com.wfp.workflow.repository.ItemRepository;
import com.wfp.workflow.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final String KEY_PATTERN = "[A-Z][A-Z0-9]{1,9}";

    private final ProjectRepository projectRepository;
    private final WorkflowEngine engine;
    private final AuditService auditService;
    private final ItemRepository itemRepository;
    private final FieldSchemaRepository fieldSchemaRepository;

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
        requireUniqueItemTypeNames(request.getItemTypes());
        request.getItemTypes().forEach(type -> requireTrackerWorkflow(tenantId, type));

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

    @Transactional
    public ProjectDto update(String key, UpdateProjectRequest request, String actor) {
        Project project = projectRepository.findByKeyForUpdate(key)
                .orElseThrow(() -> new NotFoundException("Project", key));
        requireUniqueItemTypeNames(request.getItemTypes());

        Map<String, ItemType> savedTypes = project.getItemTypes().stream()
                .collect(Collectors.toMap(ItemType::getName, Function.identity()));
        Set<String> requestedNames = request.getItemTypes().stream()
                .map(ProjectDto.ItemTypeDto::getName)
                .collect(Collectors.toSet());
        request.getItemTypes().forEach(type -> keepOrAddItemType(project, savedTypes.get(type.getName()), type));
        savedTypes.values().stream()
                .filter(type -> !requestedNames.contains(type.getName()))
                .forEach(type -> removeItemType(project, type));
        project.setName(request.getName());

        auditService.record("project.updated", "PROJECT", project.getKey(), actor,
                Map.of("name", project.getName(), "itemTypes", requestedNames));
        return toDto(project);
    }

    private void keepOrAddItemType(Project project, ItemType saved, ProjectDto.ItemTypeDto requested) {
        if (saved == null) {
            requireTrackerWorkflow(project.getTenantId(), requested);
            project.getItemTypes().add(ItemType.builder()
                    .project(project).name(requested.getName()).workflowKey(requested.getWorkflowKey()).build());
        } else if (!saved.getWorkflowKey().equals(requested.getWorkflowKey())) {
            throw new BadRequestException("The workflow of item type " + saved.getName() + " cannot be changed");
        }
    }

    private void removeItemType(Project project, ItemType type) {
        if (itemRepository.existsByType(type)) {
            throw new BadRequestException("Item type " + type.getName() + " still has items");
        }
        if (fieldSchemaRepository.existsByItemTypesContaining(type)) {
            throw new BadRequestException("Item type " + type.getName() + " still has field definitions");
        }
        project.getItemTypes().remove(type);
    }

    private static void requireUniqueItemTypeNames(List<ProjectDto.ItemTypeDto> itemTypes) {
        if (itemTypes.stream().map(ProjectDto.ItemTypeDto::getName).distinct().count() < itemTypes.size()) {
            throw new BadRequestException("Item type names must be unique within a project");
        }
    }

    private void requireTrackerWorkflow(String tenantId, ProjectDto.ItemTypeDto type) {
        engine.describe(engine.latestVersion(tenantId, type.getWorkflowKey()));
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
