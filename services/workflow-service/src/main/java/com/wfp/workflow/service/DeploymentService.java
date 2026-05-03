package com.wfp.workflow.service;

import com.wfp.common.exception.BadRequestException;
import com.wfp.common.exception.NotFoundException;
import com.wfp.security.context.TenantContext;
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

    public Deployment deploy(String name, String category, String bpmnXml) {
        String tenantId = TenantContext.requireCurrentTenantId();
        // Ensure process definitions are marked executable (Flowable requires this)
        String fixedXml = bpmnXml.replace("isExecutable=\"false\"", "isExecutable=\"true\"");
        try {
            return repositoryService.createDeployment()
                    .name(name)
                    .category(category)
                    .addString(name + ".bpmn20.xml", fixedXml)
                    .tenantId(tenantId)
                    .deploy();
        } catch (Exception e) {
            throw new BadRequestException("Invalid BPMN: " + e.getMessage());
        }
    }

    public List<ProcessDefinition> listProcessDefinitions() {
        String tenantId = TenantContext.requireCurrentTenantId();
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(tenantId)
                .latestVersion()
                .orderByProcessDefinitionName()
                .asc()
                .list();
    }

    public ProcessDefinition getProcessDefinition(String processDefinitionId) {
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
        repositoryService.deleteDeployment(deploymentId, true);
    }
}
