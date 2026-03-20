package com.workflowplatform.engine.web;

import com.workflowplatform.engine.dto.*;
import com.workflowplatform.engine.service.WorkflowEventPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor

public class TaskController {

    private final TaskService taskService;
    private final WorkflowEventPublisher eventPublisher;

    @GetMapping
    
    public ResponseEntity<PagedResponse<TaskDto>> listTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String processInstanceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String processDefinitionKey,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");

        TaskQuery query = taskService.createTaskQuery()
            .taskTenantId(tenantId)
            .orderByTaskCreateTime().desc();

        if (assignee != null && !assignee.isBlank()) {
            query.taskAssignee(assignee);
        }
        if (processInstanceId != null && !processInstanceId.isBlank()) {
            query.processInstanceId(processInstanceId);
        }
        if (processDefinitionKey != null && !processDefinitionKey.isBlank()) {
            query.processDefinitionKey(processDefinitionKey);
        }
        if ("unassigned".equalsIgnoreCase(status)) {
            query.taskUnassigned();
        } else if ("assigned".equalsIgnoreCase(status)) {
            query.taskAssigned();
        }

        long total = query.count();
        List<Task> tasks = query.listPage(page * size, size);

        List<TaskDto> dtos = tasks.stream().map(this::toDto).toList();

        return ResponseEntity.ok(PagedResponse.of(dtos, total, page, size));
    }

    @GetMapping("/{id}")
    
    public ResponseEntity<TaskDto> getTask(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");

        Task task = taskService.createTaskQuery()
            .taskId(id)
            .taskTenantId(tenantId)
            .singleResult();

        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toDto(task));
    }

    @PostMapping("/{id}/claim")
    
    public ResponseEntity<Void> claimTask(
            @PathVariable String id,
            @Valid @RequestBody ClaimTaskRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");

        Task task = getTaskForTenant(id, tenantId);

        taskService.claim(id, request.getUserId());
        eventPublisher.publishTaskClaimed(id, task.getProcessInstanceId(), request.getUserId(), tenantId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/claim")
    public ResponseEntity<Void> unclaimTask(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        getTaskForTenant(id, tenantId);

        taskService.unclaim(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    
    public ResponseEntity<Void> completeTask(
            @PathVariable String id,
            @RequestBody(required = false) CompleteTaskRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        String actorId = jwt.getSubject();

        Task task = getTaskForTenant(id, tenantId);

        var variables = (request != null && request.getVariables() != null)
            ? request.getVariables()
            : Collections.<String, Object>emptyMap();

        if (request != null && request.getComment() != null) {
            taskService.addComment(id, task.getProcessInstanceId(), request.getComment());
        }

        taskService.complete(id, variables);

        eventPublisher.publishTaskCompleted(
            id,
            task.getName(),
            task.getProcessInstanceId(),
            tenantId,
            actorId,
            variables
        );

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/delegate")
    
    public ResponseEntity<Void> delegateTask(
            @PathVariable String id,
            @Valid @RequestBody DelegateTaskRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        String actorId = jwt.getSubject();

        Task task = getTaskForTenant(id, tenantId);

        taskService.delegateTask(id, request.getUserId());
        eventPublisher.publishTaskDelegated(id, task.getProcessInstanceId(), request.getUserId(), tenantId, actorId);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/assignee")
    
    public ResponseEntity<Void> assignTask(
            @PathVariable String id,
            @Valid @RequestBody ClaimTaskRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        String actorId = jwt.getSubject();

        Task task = getTaskForTenant(id, tenantId);

        taskService.setAssignee(id, request.getUserId());
        eventPublisher.publishTaskAssigned(
            id,
            task.getName(),
            task.getProcessInstanceId(),
            request.getUserId(),
            tenantId,
            actorId
        );

        return ResponseEntity.noContent().build();
    }

    private Task getTaskForTenant(String taskId, String tenantId) {
        Task task = taskService.createTaskQuery()
            .taskId(taskId)
            .taskTenantId(tenantId)
            .singleResult();
        if (task == null) {
            throw new jakarta.persistence.EntityNotFoundException("Task not found: " + taskId);
        }
        return task;
    }

    private TaskDto toDto(Task task) {
        return TaskDto.builder()
            .id(task.getId())
            .name(task.getName())
            .description(task.getDescription())
            .assignee(task.getAssignee())
            .owner(task.getOwner())
            .processInstanceId(task.getProcessInstanceId())
            .processDefinitionId(task.getProcessDefinitionId())
            .executionId(task.getExecutionId())
            .formKey(task.getFormKey())
            .category(task.getCategory())
            .tenantId(task.getTenantId())
            .priority(task.getPriority())
            .suspended(task.isSuspended())
            .createTime(task.getCreateTime() != null ? task.getCreateTime().toInstant() : null)
            .claimTime(task.getClaimTime() != null ? task.getClaimTime().toInstant() : null)
            .dueDate(task.getDueDate() != null ? task.getDueDate().toInstant() : null)
            .status(task.getAssignee() != null ? "ASSIGNED" : "UNASSIGNED")
            .build();
    }
}
