package com.wfp.workflow.controller;

import com.wfp.common.dto.PagedResponse;
import com.wfp.workflow.dto.CompleteTaskRequest;
import com.wfp.workflow.dto.DelegateTaskRequest;
import com.wfp.workflow.dto.TaskDto;
import com.wfp.workflow.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public PagedResponse<TaskDto> list(
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String candidateGroup,
            @RequestParam(required = false) String processDefinitionKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return taskService.listTasks(assignee, candidateGroup, processDefinitionKey, page, size);
    }

    @GetMapping("/{id}")
    public TaskDto get(@PathVariable String id) {
        return taskService.getTask(id);
    }

    @PostMapping("/{id}/claim")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void claim(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        taskService.claimTask(id, jwt.getClaimAsString("preferred_username"));
    }

    @PostMapping("/{id}/unclaim")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unclaim(@PathVariable String id) {
        taskService.unclaimTask(id);
    }

    @PostMapping("/{id}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@PathVariable String id, @RequestBody(required = false) CompleteTaskRequest request,
                          @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("preferred_username");
        taskService.completeTask(id, request != null ? request.getVariables() : null, userId);
    }

    @PostMapping("/{id}/delegate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delegate(@PathVariable String id, @Valid @RequestBody DelegateTaskRequest request,
                          @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("preferred_username");
        taskService.delegateTask(id, userId, request.getDelegateToUserId(), request.getComment());
    }
}
