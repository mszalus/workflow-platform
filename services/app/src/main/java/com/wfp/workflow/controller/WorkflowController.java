package com.wfp.workflow.controller;

import com.wfp.workflow.dto.ValidateWorkflowRequest;
import com.wfp.workflow.engine.Violation;
import com.wfp.workflow.engine.WorkflowEngine;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workflow/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowEngine workflowEngine;

    @PostMapping("/validate")
    public List<Violation> validate(@Valid @RequestBody ValidateWorkflowRequest request) {
        return workflowEngine.validate(request.getBpmnXml());
    }
}
