package com.wfp.workflow.engine.flowable;

import com.wfp.common.exception.NotFoundException;
import com.wfp.security.context.TenantContext;
import com.wfp.workflow.dto.DeploymentDto;
import com.wfp.workflow.dto.ProcessDefinitionDto;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeploymentService {

    private final RepositoryService repositoryService;
    private final DeploymentXml deploymentXml = new DeploymentXml();

    public DeploymentDto deploy(String name, String category, String bpmnXml) {
        String tenantId = TenantContext.requireCurrentTenantId();
        Deployment deployment = repositoryService.createDeployment()
                .name(name)
                .category(category)
                .addString(name + ".bpmn20.xml", deploymentXml.prepare(bpmnXml))
                .tenantId(tenantId)
                .deploy();
        return DeploymentDto.builder().deploymentId(deployment.getId()).name(deployment.getName()).build();
    }

    public List<ProcessDefinitionDto> listProcessDefinitions() {
        String tenantId = TenantContext.requireCurrentTenantId();
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(tenantId)
                .latestVersion()
                .orderByProcessDefinitionName()
                .asc()
                .list()
                .stream()
                .map(this::toDto)
                .toList();
    }

    private ProcessDefinitionDto toDto(ProcessDefinition pd) {
        return ProcessDefinitionDto.builder()
                .id(pd.getId())
                .key(pd.getKey())
                .name(pd.getName() != null ? pd.getName() : pd.getKey())
                .version(pd.getVersion())
                .deploymentId(pd.getDeploymentId())
                .suspended(pd.isSuspended())
                .build();
    }

    private ProcessDefinition getProcessDefinition(String processDefinitionId) {
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .processDefinitionTenantId(TenantContext.requireCurrentTenantId())
                .singleResult();
        if (pd == null) {
            throw new NotFoundException("ProcessDefinition", processDefinitionId);
        }
        return pd;
    }

    public String getProcessDefinitionBpmnXml(String processDefinitionId) {
        ProcessDefinition pd = getProcessDefinition(processDefinitionId);
        InputStream is = repositoryService.getResourceAsStream(
                pd.getDeploymentId(), pd.getResourceName());
        if (is == null) {
            throw new NotFoundException("BPMN resource", processDefinitionId);
        }
        try {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read BPMN XML", e);
        }
    }

    public void deleteDeployment(String deploymentId) {
        requireTenantDeployment(deploymentId);
        repositoryService.deleteDeployment(deploymentId, true);
    }

    private void requireTenantDeployment(String deploymentId) {
        long matching = repositoryService.createDeploymentQuery()
                .deploymentId(deploymentId)
                .deploymentTenantId(TenantContext.requireCurrentTenantId())
                .count();
        if (matching == 0) {
            throw new NotFoundException("Deployment", deploymentId);
        }
    }
}
