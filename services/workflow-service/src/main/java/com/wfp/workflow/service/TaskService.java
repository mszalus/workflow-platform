package com.wfp.workflow.service;

import com.wfp.common.dto.PagedResponse;
import com.wfp.common.exception.NotFoundException;
import com.wfp.events.*;
import com.wfp.security.context.TenantContext;
import com.wfp.workflow.dto.TaskDto;
import lombok.RequiredArgsConstructor;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final org.flowable.engine.TaskService flowableTaskService;
    private final EventPublisher eventPublisher;

    public PagedResponse<TaskDto> listTasks(String assignee, String candidateGroup,
                                             String processDefinitionKey, int page, int size) {
        String tenantId = TenantContext.requireCurrentTenantId();
        TaskQuery query = flowableTaskService.createTaskQuery().taskTenantId(tenantId);
        if (assignee != null) query.taskAssignee(assignee);
        if (candidateGroup != null) query.taskCandidateGroup(candidateGroup);
        if (processDefinitionKey != null) query.processDefinitionKey(processDefinitionKey);
        query.orderByTaskCreateTime().desc();

        long total = query.count();
        List<TaskDto> items = query.listPage(page * size, size)
                .stream().map(this::toDto).toList();
        return PagedResponse.of(items, page, size, total);
    }

    public TaskDto getTask(String taskId) {
        Task task = flowableTaskService.createTaskQuery()
                .taskId(taskId)
                .taskTenantId(TenantContext.requireCurrentTenantId())
                .singleResult();
        if (task == null) throw new NotFoundException("Task", taskId);
        return toDto(task);
    }

    public void claimTask(String taskId, String userId) {
        flowableTaskService.claim(taskId, userId);
    }

    public void unclaimTask(String taskId) {
        flowableTaskService.unclaim(taskId);
    }

    public void completeTask(String taskId, Map<String, Object> variables, String userId) {
        String tenantId = TenantContext.requireCurrentTenantId();
        Task task = flowableTaskService.createTaskQuery().taskId(taskId).singleResult();

        flowableTaskService.complete(taskId, variables != null ? variables : Map.of());

        TaskCompletedEvent event = TaskCompletedEvent.builder()
                .taskId(taskId)
                .taskName(task != null ? task.getName() : null)
                .processInstanceId(task != null ? task.getProcessInstanceId() : null)
                .completedBy(userId)
                .outcome(variables)
                .build();
        event.initDefaults(EventConstants.TASK_COMPLETED, tenantId, userId);
        eventPublisher.publish(EventConstants.TASK_COMPLETED, event);
    }

    public void delegateTask(String taskId, String fromUserId, String toUserId, String comment) {
        String tenantId = TenantContext.requireCurrentTenantId();
        Task task = flowableTaskService.createTaskQuery().taskId(taskId).singleResult();
        flowableTaskService.delegateTask(taskId, toUserId);

        TaskDelegatedEvent event = TaskDelegatedEvent.builder()
                .taskId(taskId)
                .taskName(task != null ? task.getName() : null)
                .processInstanceId(task != null ? task.getProcessInstanceId() : null)
                .delegatedFrom(fromUserId)
                .delegatedTo(toUserId)
                .comment(comment)
                .build();
        event.initDefaults(EventConstants.TASK_DELEGATED, tenantId, fromUserId);
        eventPublisher.publish(EventConstants.TASK_DELEGATED, event);
    }

    private TaskDto toDto(Task task) {
        Date dueDate = task.getDueDate();
        Date createTime = task.getCreateTime();
        return TaskDto.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .assignee(task.getAssignee())
                .owner(task.getOwner())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .createTime(createTime != null ? createTime.toInstant() : null)
                .dueDate(dueDate != null ? dueDate.toInstant() : null)
                .priority(task.getPriority())
                .tenantId(task.getTenantId())
                .formKey(task.getFormKey())
                .build();
    }
}
