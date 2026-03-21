package com.wfp.workflow.controller;

import com.wfp.workflow.dto.DeployProcessRequest;
import com.wfp.workflow.service.DeploymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> deploy(@Valid @RequestBody DeployProcessRequest request) {
        Deployment d = deploymentService.deploy(request.getName(), request.getCategory(), request.getBpmnXml());
        return Map.of("deploymentId", d.getId(), "name", d.getName());
    }

    @GetMapping
    public List<Map<String, Object>> listProcessDefinitions() {
        return deploymentService.listProcessDefinitions().stream()
                .map(pd -> Map.<String, Object>of(
                        "id", pd.getId(),
                        "key", pd.getKey(),
                        "name", pd.getName() != null ? pd.getName() : pd.getKey(),
                        "version", pd.getVersion(),
                        "deploymentId", pd.getDeploymentId(),
                        "suspended", pd.isSuspended()
                )).toList();
    }

    @GetMapping("/{processDefinitionId}/bpmn")
    public Map<String, String> getBpmnXml(@PathVariable String processDefinitionId) {
        String xml = deploymentService.getProcessDefinitionBpmnXml(processDefinitionId);
        return Map.of("bpmnXml", xml);
    }

    @DeleteMapping("/{deploymentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeployment(@PathVariable String deploymentId) {
        deploymentService.deleteDeployment(deploymentId);
    }
}
