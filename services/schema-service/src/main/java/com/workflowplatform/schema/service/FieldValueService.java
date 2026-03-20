package com.workflowplatform.schema.service;

import com.workflowplatform.schema.domain.FieldValue;
import com.workflowplatform.schema.dto.FieldValueDto;
import com.workflowplatform.schema.dto.SaveFieldValuesRequest;
import com.workflowplatform.schema.repository.FieldValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FieldValueService {

    private final FieldValueRepository fieldValueRepository;

    public List<FieldValueDto> saveValues(String tenantId, SaveFieldValuesRequest request) {
        List<FieldValue> saved = request.getValues().stream()
            .map(dto -> {
                FieldValue existing = (request.getTaskId() != null)
                    ? fieldValueRepository.findByTenantIdAndProcessInstanceIdAndTaskIdAndFieldKey(
                        tenantId, request.getProcessInstanceId(), request.getTaskId(), dto.getFieldKey())
                        .orElse(null)
                    : fieldValueRepository.findByTenantIdAndProcessInstanceIdAndFieldKey(
                        tenantId, request.getProcessInstanceId(), dto.getFieldKey())
                        .orElse(null);

                FieldValue fieldValue = existing != null ? existing : FieldValue.builder()
                    .tenantId(tenantId)
                    .processInstanceId(request.getProcessInstanceId())
                    .taskId(request.getTaskId())
                    .fieldKey(dto.getFieldKey())
                    .fieldType(dto.getFieldType())
                    .build();

                fieldValue.setFieldType(dto.getFieldType());
                fieldValue.setValueText(dto.getValueText());
                fieldValue.setValueNumber(dto.getValueNumber());
                fieldValue.setValueDate(dto.getValueDate());
                fieldValue.setValueBoolean(dto.getValueBoolean());
                fieldValue.setValueJson(dto.getValueJson());

                return fieldValueRepository.save(fieldValue);
            })
            .toList();

        log.info("Saved {} field values for processInstanceId={} taskId={} tenant={}",
            saved.size(), request.getProcessInstanceId(), request.getTaskId(), tenantId);
        return saved.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<FieldValueDto> getValues(String tenantId, String processInstanceId, String taskId) {
        List<FieldValue> values = (taskId != null)
            ? fieldValueRepository.findByTenantIdAndProcessInstanceIdAndTaskId(tenantId, processInstanceId, taskId)
            : fieldValueRepository.findByTenantIdAndProcessInstanceId(tenantId, processInstanceId);
        return values.stream().map(this::toDto).toList();
    }

    public void deleteValues(String tenantId, String processInstanceId) {
        int deleted = fieldValueRepository.deleteByTenantIdAndProcessInstanceId(tenantId, processInstanceId);
        log.info("Deleted {} field values for processInstanceId={} tenant={}", deleted, processInstanceId, tenantId);
    }

    private Object extractValue(FieldValue fv) {
        if (fv.getFieldType() == null) return null;
        return switch (fv.getFieldType()) {
            case TEXT, ENUM -> fv.getValueText();
            case NUMBER -> fv.getValueNumber();
            case DATE -> fv.getValueDate();
            case BOOLEAN -> fv.getValueBoolean();
            case FILE_REF, USER_REF -> fv.getValueJson();
        };
    }

    private FieldValueDto toDto(FieldValue v) {
        return FieldValueDto.builder()
            .id(v.getId())
            .tenantId(v.getTenantId())
            .processInstanceId(v.getProcessInstanceId())
            .taskId(v.getTaskId())
            .fieldKey(v.getFieldKey())
            .fieldType(v.getFieldType())
            .valueText(v.getValueText())
            .valueNumber(v.getValueNumber())
            .valueDate(v.getValueDate())
            .valueBoolean(v.getValueBoolean())
            .valueJson(v.getValueJson())
            .createdAt(v.getCreatedAt())
            .updatedAt(v.getUpdatedAt())
            .build();
    }
}
