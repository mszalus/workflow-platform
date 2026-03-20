package com.wfp.workflow.service;

import com.wfp.common.dto.PagedResponse;
import com.wfp.common.exception.NotFoundException;
import com.wfp.events.EventConstants;
import com.wfp.events.ProcessStartedEvent;
import com.wfp.security.context.TenantContext;
import com.wfp.workflow.dto.ProcessInstanceDto;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProcessService {

    private final RuntimeService runtimeService;
    private final EventPublisher eventPublisher;

    public ProcessInstanceDto startProcess(String processDefinitionKey, String businessKey,
                                            Map<String, Object> variables, String userId) {
        String tenantId = TenantContext.requireCurrentTenantId();
        ProcessInstance pi = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey(processDefinitionKey)
                .businessKey(businessKey)
                .variables(variables != null ? variables : Map.of())
                .tenantId(tenantId)
                .start();

        ProcessStartedEvent event = ProcessStartedEvent.builder()
                .processInstanceId(pi.getId())
                .processDefinitionId(pi.getProcessDefinitionId())
                .processDefinitionKey(pi.getProcessDefinitionKey())
                .processName(pi.getProcessDefinitionName())
                .businessKey(businessKey)
                .variables(variables)
                .build();
        event.initDefaults(EventConstants.PROCESS_STARTED, tenantId, userId);
        eventPublisher.publish(EventConstants.PROCESS_STARTED, event);

        return toDto(pi);
    }

    public PagedResponse<ProcessInstanceDto> listInstances(int page, int size) {
        String tenantId = TenantContext.requireCurrentTenantId();
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
                .processInstanceTenantId(tenantId)
                .orderByProcessInstanceId().desc();

        long total = query.count();
        List<ProcessInstanceDto> items = query.listPage(page * size, size)
                .stream().map(this::toDto).toList();
        return PagedResponse.of(items, page, size, total);
    }

    public ProcessInstanceDto getInstance(String processInstanceId) {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .processInstanceTenantId(TenantContext.requireCurrentTenantId())
                .singleResult();
        if (pi == null) throw new NotFoundException("ProcessInstance", processInstanceId);
        return toDto(pi);
    }

    public void cancelProcess(String processInstanceId, String reason) {
        runtimeService.deleteProcessInstance(processInstanceId, reason);
    }

    private ProcessInstanceDto toDto(ProcessInstance pi) {
        Date startTime = pi.getStartTime();
        return ProcessInstanceDto.builder()
                .id(pi.getId())
                .processDefinitionId(pi.getProcessDefinitionId())
                .processDefinitionKey(pi.getProcessDefinitionKey())
                .processDefinitionName(pi.getProcessDefinitionName())
                .businessKey(pi.getBusinessKey())
                .startTime(startTime != null ? startTime.toInstant() : null)
                .startUserId(pi.getStartUserId())
                .tenantId(pi.getTenantId())
                .build();
    }
}
