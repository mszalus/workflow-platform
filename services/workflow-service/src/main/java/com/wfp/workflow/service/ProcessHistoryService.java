package com.wfp.workflow.service;

import com.wfp.common.dto.PagedResponse;
import com.wfp.security.context.TenantContext;
import com.wfp.workflow.dto.ProcessInstanceDto;
import com.wfp.workflow.dto.TaskDto;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcessHistoryService {

    private final org.flowable.engine.HistoryService flowableHistoryService;

    public PagedResponse<ProcessInstanceDto> listCompletedProcesses(int page, int size) {
        String tenantId = TenantContext.requireCurrentTenantId();
        HistoricProcessInstanceQuery query = flowableHistoryService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(tenantId)
                .finished()
                .orderByProcessInstanceEndTime().desc();

        long total = query.count();
        List<ProcessInstanceDto> items = query.listPage(page * size, size)
                .stream().map(this::toProcessDto).toList();
        return PagedResponse.of(items, page, size, total);
    }

    public PagedResponse<TaskDto> listCompletedTasks(String processInstanceId, int page, int size) {
        String tenantId = TenantContext.requireCurrentTenantId();
        HistoricTaskInstanceQuery query = flowableHistoryService.createHistoricTaskInstanceQuery()
                .taskTenantId(tenantId)
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc();

        if (processInstanceId != null) {
            query.processInstanceId(processInstanceId);
        }

        long total = query.count();
        List<TaskDto> items = query.listPage(page * size, size)
                .stream().map(this::toTaskDto).toList();
        return PagedResponse.of(items, page, size, total);
    }

    private ProcessInstanceDto toProcessDto(HistoricProcessInstance hpi) {
        return ProcessInstanceDto.builder()
                .id(hpi.getId())
                .processDefinitionId(hpi.getProcessDefinitionId())
                .processDefinitionKey(hpi.getProcessDefinitionKey())
                .processDefinitionName(hpi.getProcessDefinitionName())
                .businessKey(hpi.getBusinessKey())
                .startTime(hpi.getStartTime() != null ? hpi.getStartTime().toInstant() : null)
                .endTime(hpi.getEndTime() != null ? hpi.getEndTime().toInstant() : null)
                .startUserId(hpi.getStartUserId())
                .tenantId(hpi.getTenantId())
                .build();
    }

    private String extractProcessDefinitionKey(String processDefinitionId) {
        if (processDefinitionId == null) {
            return null;
        }
        int colonIdx = processDefinitionId.indexOf(':');
        return colonIdx > 0 ? processDefinitionId.substring(0, colonIdx) : processDefinitionId;
    }

    private TaskDto toTaskDto(HistoricTaskInstance hti) {
        return TaskDto.builder()
                .id(hti.getId())
                .name(hti.getName())
                .description(hti.getDescription())
                .assignee(hti.getAssignee())
                .processInstanceId(hti.getProcessInstanceId())
                .processDefinitionId(hti.getProcessDefinitionId())
                .processDefinitionKey(extractProcessDefinitionKey(hti.getProcessDefinitionId()))
                .taskDefinitionKey(hti.getTaskDefinitionKey())
                .createTime(hti.getCreateTime() != null ? hti.getCreateTime().toInstant() : null)
                .dueDate(hti.getDueDate() != null ? hti.getDueDate().toInstant() : null)
                .priority(hti.getPriority())
                .tenantId(hti.getTenantId())
                .build();
    }
}
