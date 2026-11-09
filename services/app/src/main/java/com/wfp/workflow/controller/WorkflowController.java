package com.wfp.workflow.controller;

import com.wfp.workflow.dto.UpgradeSummary;
import com.wfp.workflow.dto.ValidateWorkflowRequest;
import com.wfp.workflow.engine.Violation;
import com.wfp.workflow.engine.WorkflowEngine;
import com.wfp.workflow.service.WorkflowUpgradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final WorkflowUpgradeService upgradeService;

    @PostMapping("/validate")
    public List<Violation> validate(@Valid @RequestBody ValidateWorkflowRequest request) {
        return workflowEngine.validate(request.getBpmnXml());
    }

    @PostMapping("/upgrade-preview")
    public UpgradeSummary previewUpgrade(@Valid @RequestBody ValidateWorkflowRequest request) {
        return upgradeService.preview(request.getBpmnXml());
    }

    @PostMapping("/{key}/upgrade")
    public UpgradeSummary upgrade(@PathVariable String key, @AuthenticationPrincipal Jwt jwt) {
        return upgradeService.moveOpenItems(key, jwt.getClaimAsString("preferred_username"));
    }
}
