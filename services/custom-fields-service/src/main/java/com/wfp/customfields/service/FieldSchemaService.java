package com.wfp.customfields.service;

import com.wfp.common.exception.NotFoundException;
import com.wfp.customfields.dto.CreateFieldSchemaRequest;
import com.wfp.customfields.dto.FieldSchemaDto;
import com.wfp.customfields.entity.FieldOption;
import com.wfp.customfields.entity.FieldSchema;
import com.wfp.customfields.repository.FieldSchemaRepository;
import com.wfp.security.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FieldSchemaService {

    private final FieldSchemaRepository schemaRepository;

    @Transactional
    public FieldSchemaDto createSchema(CreateFieldSchemaRequest req) {
        String tenantId = TenantContext.requireCurrentTenantId();
        FieldSchema schema = FieldSchema.builder()
                .processDefinitionKey(req.getProcessDefinitionKey())
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
        return toDto(schemaRepository.save(schema));
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

    @Transactional
    public void deleteSchema(UUID id) {
        String tenantId = TenantContext.requireCurrentTenantId();
        FieldSchema schema = schemaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("FieldSchema", id));
        schemaRepository.delete(schema);
    }

    private FieldSchemaDto toDto(FieldSchema s) {
        return FieldSchemaDto.builder()
                .id(s.getId().toString())
                .processDefinitionKey(s.getProcessDefinitionKey())
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
