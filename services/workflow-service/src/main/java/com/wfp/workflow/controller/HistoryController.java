package com.wfp.workflow.controller;

import com.wfp.common.dto.PagedResponse;
import com.wfp.workflow.dto.ProcessInstanceDto;
import com.wfp.workflow.dto.TaskDto;
import com.wfp.workflow.service.ProcessHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final ProcessHistoryService historyService;

    @GetMapping("/processes")
    public PagedResponse<ProcessInstanceDto> completedProcesses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return historyService.listCompletedProcesses(page, size);
    }

    @GetMapping("/tasks")
    public PagedResponse<TaskDto> completedTasks(
            @RequestParam(required = false) String processInstanceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return historyService.listCompletedTasks(processInstanceId, page, size);
    }
}
