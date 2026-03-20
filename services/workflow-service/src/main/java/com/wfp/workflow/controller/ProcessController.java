package com.wfp.workflow.controller;

import com.wfp.common.dto.PagedResponse;
import com.wfp.workflow.dto.ProcessInstanceDto;
import com.wfp.workflow.dto.StartProcessRequest;
import com.wfp.workflow.service.ProcessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/processes")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProcessInstanceDto start(@Valid @RequestBody StartProcessRequest request,
                                     @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("preferred_username");
        return processService.startProcess(request.getProcessDefinitionKey(),
                request.getBusinessKey(), request.getVariables(), userId);
    }

    @GetMapping
    public PagedResponse<ProcessInstanceDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return processService.listInstances(page, size);
    }

    @GetMapping("/{id}")
    public ProcessInstanceDto get(@PathVariable String id) {
        return processService.getInstance(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable String id, @RequestParam(defaultValue = "Cancelled by user") String reason) {
        processService.cancelProcess(id, reason);
    }
}
