package com.wfp.workflow.engine.flowable;

import com.wfp.common.exception.BadRequestException;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DeploymentService {

    private final RepositoryService repositoryService;
    private final DeploymentXml deploymentXml = new DeploymentXml();

    public DeploymentDto deploy(String name, String category, String bpmnXml) {
        String tenantId = TenantContext.requireCurrentTenantId();
        if (deploymentXml.processIds(bpmnXml).contains(BuiltInWorkflow.KEY)) {
            throw new BadRequestException(BuiltInWorkflow.KEY
                    + " is the built-in workflow; give your workflow another id");
        }
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
        List<ProcessDefinition> own = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(tenantId)
                .latestVersion()
                .list();
        List<ProcessDefinition> builtIn = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(BuiltInWorkflow.KEY)
                .processDefinitionWithoutTenantId()
                .latestVersion()
                .list();
        return Stream.concat(own.stream(), builtIn.stream())
                .sorted(Comparator.comparing(DeploymentService::displayName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDto)
                .toList();
    }

    private static String displayName(ProcessDefinition pd) {
        return pd.getName() != null ? pd.getName() : pd.getKey();
    }

    private ProcessDefinitionDto toDto(ProcessDefinition pd) {
        return ProcessDefinitionDto.builder()
                .id(pd.getId())
                .key(pd.getKey())
                .name(displayName(pd))
                .version(pd.getVersion())
                .deploymentId(pd.getDeploymentId())
                .suspended(pd.isSuspended())
                .builtIn(BuiltInWorkflow.isBuiltIn(pd))
                .build();
    }

    private ProcessDefinition getProcessDefinition(String processDefinitionId) {
        String tenantId = TenantContext.requireCurrentTenantId();
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        if (pd == null || !(BuiltInWorkflow.isBuiltIn(pd) || tenantId.equals(pd.getTenantId()))) {
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
