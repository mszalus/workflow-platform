package com.wfp.customfields.service;

import com.wfp.common.exception.BadRequestException;
import com.wfp.customfields.dto.FieldValueDto;
import com.wfp.customfields.dto.SaveFieldValuesRequest;
import com.wfp.customfields.entity.FieldSchema;
import com.wfp.customfields.entity.FieldValue;
import com.wfp.customfields.repository.FieldSchemaRepository;
import com.wfp.customfields.repository.FieldValueRepository;
import com.wfp.security.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class FieldValueService {

    private final FieldValueRepository valueRepository;
    private final FieldSchemaRepository schemaRepository;

    @Transactional
    public void saveValues(SaveFieldValuesRequest req) {
        String tenantId = TenantContext.requireCurrentTenantId();
        String pdKey = req.getProcessDefinitionKey();

        List<FieldSchema> schemas = schemaRepository
                .findByProcessDefinitionKeyAndTenantIdOrderBySortOrder(pdKey, tenantId);
        Map<String, FieldSchema> schemaMap = new HashMap<>();
        schemas.forEach(s -> schemaMap.put(s.getFieldKey(), s));

        for (FieldSchema schema : schemas) {
            if (schema.isRequired() && !req.getValues().containsKey(schema.getFieldKey())) {
                throw new BadRequestException("Field '" + schema.getFieldKey() + "' is required");
            }
        }

        for (Map.Entry<String, String> entry : req.getValues().entrySet()) {
            FieldSchema schema = schemaMap.get(entry.getKey());
            if (schema == null) continue;

            if (schema.getValidationRegex() != null && entry.getValue() != null
                    && !entry.getValue().matches(schema.getValidationRegex())) {
                throw new BadRequestException("Field '" + entry.getKey() + "' failed validation");
            }

            Optional<FieldValue> existing = valueRepository
                    .findByFieldSchemaIdAndProcessInstanceIdAndTenantId(
                            schema.getId(), req.getProcessInstanceId(), tenantId);

            if (existing.isPresent()) {
                existing.get().setValue(entry.getValue());
                valueRepository.save(existing.get());
            } else {
                valueRepository.save(FieldValue.builder()
                        .fieldSchemaId(schema.getId())
                        .processInstanceId(req.getProcessInstanceId())
                        .value(entry.getValue())
                        .tenantId(tenantId)
                        .build());
            }
        }
    }

    public List<FieldValueDto> getValues(String processInstanceId) {
        String tenantId = TenantContext.requireCurrentTenantId();
        List<FieldValue> values = valueRepository
                .findByProcessInstanceIdAndTenantId(processInstanceId, tenantId);

        return values.stream().map(v -> {
            FieldSchema schema = schemaRepository.findById(v.getFieldSchemaId()).orElse(null);
            return FieldValueDto.builder()
                    .fieldSchemaId(v.getFieldSchemaId().toString())
                    .fieldKey(schema != null ? schema.getFieldKey() : null)
                    .label(schema != null ? schema.getLabel() : null)
                    .fieldType(schema != null ? schema.getFieldType() : null)
                    .value(v.getValue())
                    .build();
        }).toList();
    }
}
