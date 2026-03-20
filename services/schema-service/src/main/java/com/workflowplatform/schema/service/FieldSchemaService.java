package com.workflowplatform.schema.service;

import com.workflowplatform.schema.domain.FieldDefinition;
import com.workflowplatform.schema.domain.FieldSchema;
import com.workflowplatform.schema.dto.CreateSchemaRequest;
import com.workflowplatform.schema.dto.FieldDefinitionDto;
import com.workflowplatform.schema.dto.FieldSchemaDto;
import com.workflowplatform.schema.repository.FieldSchemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FieldSchemaService {

    private final FieldSchemaRepository schemaRepository;

    public FieldSchemaDto createSchema(String tenantId, CreateSchemaRequest request) {
        int nextVersion = schemaRepository
            .findMaxVersionByTenantIdAndProcessDefKey(tenantId, request.getProcessDefKey())
            .map(v -> v + 1)
            .orElse(1);

        FieldSchema schema = FieldSchema.builder()
            .tenantId(tenantId)
            .processDefKey(request.getProcessDefKey())
            .version(nextVersion)
            .isActive(true)
            .description(request.getDescription())
            .build();

        if (request.getFields() != null) {
            for (FieldDefinitionDto fieldDto : request.getFields()) {
                schema.addField(toDefinitionEntity(fieldDto));
            }
        }

        FieldSchema saved = schemaRepository.save(schema);
        schemaRepository.deactivateOtherVersions(tenantId, request.getProcessDefKey(), saved.getId());

        log.info("Created schema id={} tenant={} processDefKey={} version={}",
            saved.getId(), tenantId, saved.getProcessDefKey(), saved.getVersion());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public FieldSchemaDto getActiveSchema(String tenantId, String processDefKey) {
        FieldSchema schema = schemaRepository
            .findByTenantIdAndProcessDefKeyAndIsActiveTrue(tenantId, processDefKey)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No active schema found for processDefKey: " + processDefKey));
        return toDto(schema);
    }

    @Transactional(readOnly = true)
    public FieldSchemaDto getSchema(String tenantId, UUID schemaId) {
        FieldSchema schema = schemaRepository.findById(schemaId)
            .filter(s -> s.getTenantId().equals(tenantId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Schema not found: " + schemaId));
        return toDto(schema);
    }

    @Transactional(readOnly = true)
    public Page<FieldSchemaDto> listSchemas(String tenantId, String processDefKey, Pageable pageable) {
        if (processDefKey != null) {
            return schemaRepository
                .findByTenantIdAndProcessDefKey(tenantId, processDefKey, pageable)
                .map(this::toDto);
        }
        return schemaRepository.findByTenantId(tenantId, pageable).map(this::toDto);
    }

    public FieldSchemaDto updateSchema(String tenantId, UUID schemaId, CreateSchemaRequest request) {
        FieldSchema existing = schemaRepository.findById(schemaId)
            .filter(s -> s.getTenantId().equals(tenantId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Schema not found: " + schemaId));

        int nextVersion = schemaRepository
            .findMaxVersionByTenantIdAndProcessDefKey(tenantId, existing.getProcessDefKey())
            .map(v -> v + 1)
            .orElse(existing.getVersion() + 1);

        FieldSchema newSchema = FieldSchema.builder()
            .tenantId(tenantId)
            .processDefKey(existing.getProcessDefKey())
            .version(nextVersion)
            .isActive(true)
            .description(request.getDescription() != null ? request.getDescription() : existing.getDescription())
            .build();

        if (request.getFields() != null) {
            for (FieldDefinitionDto fieldDto : request.getFields()) {
                newSchema.addField(toDefinitionEntity(fieldDto));
            }
        }

        FieldSchema saved = schemaRepository.save(newSchema);
        schemaRepository.deactivateOtherVersions(tenantId, existing.getProcessDefKey(), saved.getId());

        log.info("Updated schema: created new version id={} version={} tenant={} processDefKey={}",
            saved.getId(), saved.getVersion(), tenantId, saved.getProcessDefKey());
        return toDto(saved);
    }

    public void deleteSchema(String tenantId, UUID schemaId) {
        FieldSchema schema = schemaRepository.findById(schemaId)
            .filter(s -> s.getTenantId().equals(tenantId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Schema not found: " + schemaId));

        schema.setActive(false);
        schemaRepository.save(schema);
        log.info("Soft-deleted (deactivated) schema id={} tenant={}", schemaId, tenantId);
    }

    // --- Mapping helpers ---

    private FieldSchemaDto toDto(FieldSchema schema) {
        List<FieldDefinitionDto> fieldDtos = schema.getFields().stream()
            .map(this::toDefinitionDto)
            .toList();

        return FieldSchemaDto.builder()
            .id(schema.getId())
            .tenantId(schema.getTenantId())
            .processDefKey(schema.getProcessDefKey())
            .version(schema.getVersion())
            .active(schema.isActive())
            .description(schema.getDescription())
            .fields(fieldDtos)
            .createdAt(schema.getCreatedAt())
            .updatedAt(schema.getUpdatedAt())
            .build();
    }

    private FieldDefinitionDto toDefinitionDto(FieldDefinition fd) {
        return FieldDefinitionDto.builder()
            .id(fd.getId())
            .fieldKey(fd.getFieldKey())
            .label(fd.getLabel())
            .fieldType(fd.getFieldType())
            .required(fd.isRequired())
            .validationRules(fd.getValidationRules())
            .options(fd.getOptions())
            .visibleOnTasks(fd.getVisibleOnTasks())
            .editableByRoles(fd.getEditableByRoles())
            .displayOrder(fd.getDisplayOrder())
            .placeholder(fd.getPlaceholder())
            .helpText(fd.getHelpText())
            .build();
    }

    private FieldDefinition toDefinitionEntity(FieldDefinitionDto dto) {
        return FieldDefinition.builder()
            .fieldKey(dto.getFieldKey())
            .label(dto.getLabel())
            .fieldType(dto.getFieldType())
            .required(dto.isRequired())
            .validationRules(dto.getValidationRules())
            .options(dto.getOptions())
            .visibleOnTasks(dto.getVisibleOnTasks())
            .editableByRoles(dto.getEditableByRoles())
            .displayOrder(dto.getDisplayOrder())
            .placeholder(dto.getPlaceholder())
            .helpText(dto.getHelpText())
            .build();
    }
}
