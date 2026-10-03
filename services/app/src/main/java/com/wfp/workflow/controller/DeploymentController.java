package com.wfp.workflow.controller;

import com.wfp.common.dto.ErrorResponse;
import com.wfp.workflow.dto.DeployProcessRequest;
import com.wfp.workflow.dto.DeploymentDto;
import com.wfp.workflow.dto.ProcessDefinitionDto;
import com.wfp.workflow.engine.InvalidWorkflowException;
import com.wfp.workflow.engine.flowable.DeploymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeploymentDto deploy(@Valid @RequestBody DeployProcessRequest request) {
        return deploymentService.deploy(request.getName(), request.getCategory(), request.getBpmnXml());
    }

    @GetMapping
    public List<ProcessDefinitionDto> listProcessDefinitions() {
        return deploymentService.listProcessDefinitions();
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

    @ExceptionHandler(InvalidWorkflowException.class)
    public ResponseEntity<ErrorResponse> handleInvalidWorkflow(InvalidWorkflowException ex,
                                                               HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .details(Map.of("violations", ex.getViolations()))
                .build());
    }
}
