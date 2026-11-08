package com.wfp.workflow.service;

import com.wfp.common.exception.BadRequestException;
import com.wfp.common.exception.NotFoundException;
import com.wfp.workflow.dto.CreateFieldSchemaRequest;
import com.wfp.workflow.dto.FieldSchemaDto;
import com.wfp.workflow.entity.FieldOption;
import com.wfp.workflow.entity.FieldSchema;
import com.wfp.workflow.entity.ItemType;
import com.wfp.workflow.entity.Project;
import com.wfp.workflow.repository.FieldSchemaRepository;
import com.wfp.workflow.repository.ProjectRepository;
import com.wfp.security.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FieldSchemaService {

    private final FieldSchemaRepository schemaRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public FieldSchemaDto createSchema(CreateFieldSchemaRequest req) {
        String tenantId = TenantContext.requireCurrentTenantId();
        boolean scopedToProject = StringUtils.hasText(req.getProject());
        if (scopedToProject == StringUtils.hasText(req.getProcessDefinitionKey())) {
            throw new BadRequestException("Give either a project or a processDefinitionKey");
        }
        FieldSchema schema = FieldSchema.builder()
                .processDefinitionKey(scopedToProject ? null : req.getProcessDefinitionKey())
                .fieldKey(req.getFieldKey())
                .label(req.getLabel())
                .fieldType(req.getFieldType())
                .required(req.isRequired())
                .sortOrder(req.getSortOrder())
                .defaultValue(req.getDefaultValue())
                .placeholder(req.getPlaceholder())
                .validationRegex(req.getValidationRegex())
                .tenantId(tenantId)
                .build();

        if (req.getOptions() != null) {
            for (var opt : req.getOptions()) {
                FieldOption fo = FieldOption.builder()
                        .fieldSchema(schema)
                        .label(opt.getLabel())
                        .value(opt.getValue())
                        .sortOrder(opt.getSortOrder())
                        .build();
                schema.getOptions().add(fo);
            }
        }
        if (scopedToProject) {
            scopeToProject(schema, req);
        }
        return toDto(schemaRepository.save(schema));
    }

    @Transactional(readOnly = true)
    public List<FieldSchemaDto> listForProject(String projectKey, String itemTypeName) {
        Project project = requireProject(projectKey);
        List<FieldSchema> schemas = itemTypeName == null
                ? schemaRepository.findByProjectOrderBySortOrder(project)
                : schemaRepository.findApplicableTo(project, requireItemType(project, itemTypeName));
        return schemas.stream().map(this::toDto).toList();
    }

    public FieldSchemaDto getSchema(UUID id) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return schemaRepository.findByIdAndTenantId(id, tenantId)
                .map(this::toDto)
                .orElseThrow(() -> new NotFoundException("FieldSchema", id));
    }

    public List<FieldSchemaDto> listByProcessDefinition(String processDefinitionKey) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return schemaRepository.findByProcessDefinitionKeyAndTenantIdOrderBySortOrder(processDefinitionKey, tenantId)
                .stream().map(this::toDto).toList();
    }

    public List<FieldSchemaDto> listAll() {
        String tenantId = TenantContext.requireCurrentTenantId();
        return schemaRepository.findByTenantIdOrderBySortOrder(tenantId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public void deleteSchema(UUID id) {
        String tenantId = TenantContext.requireCurrentTenantId();
        FieldSchema schema = schemaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("FieldSchema", id));
        schemaRepository.delete(schema);
    }

    private void scopeToProject(FieldSchema schema, CreateFieldSchemaRequest req) {
        Project project = requireProject(req.getProject());
        Set<ItemType> itemTypes = Objects.requireNonNullElse(req.getItemTypes(), List.<String>of()).stream()
                .map(name -> requireItemType(project, name))
                .collect(Collectors.toSet());
        boolean clashes = schemaRepository.findByProjectAndFieldKey(project, req.getFieldKey()).stream()
                .anyMatch(existing -> shareAnItemType(existing.getItemTypes(), itemTypes));
        if (clashes) {
            throw new BadRequestException(
                    "Project " + project.getKey() + " already has field '" + req.getFieldKey()
                    + "' for these item types");
        }
        schema.setProject(project);
        schema.getItemTypes().addAll(itemTypes);
    }

    private static boolean shareAnItemType(Set<ItemType> first, Set<ItemType> second) {
        return first.isEmpty() || second.isEmpty() || !Collections.disjoint(first, second);
    }

    private Project requireProject(String key) {
        return projectRepository.findByKey(key).orElseThrow(() -> new NotFoundException("Project", key));
    }

    private static ItemType requireItemType(Project project, String name) {
        return project.getItemTypes().stream()
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Project " + project.getKey() + " has no item type " + name));
    }

    private FieldSchemaDto toDto(FieldSchema s) {
        return FieldSchemaDto.builder()
                .id(s.getId().toString())
                .processDefinitionKey(s.getProcessDefinitionKey())
                .project(s.getProject() == null ? null : s.getProject().getKey())
                .itemTypes(s.getItemTypes().stream().map(ItemType::getName).sorted().toList())
                .fieldKey(s.getFieldKey())
                .label(s.getLabel())
                .fieldType(s.getFieldType())
                .required(s.isRequired())
                .sortOrder(s.getSortOrder())
                .defaultValue(s.getDefaultValue())
                .placeholder(s.getPlaceholder())
                .validationRegex(s.getValidationRegex())
                .options(s.getOptions().stream().map(o -> FieldSchemaDto.OptionDto.builder()
                        .id(o.getId().toString())
                        .label(o.getLabel())
                        .value(o.getValue())
                        .sortOrder(o.getSortOrder())
                        .build()).toList())
                .build();
    }
}
