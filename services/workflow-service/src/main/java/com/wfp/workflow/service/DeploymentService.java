package com.wfp.workflow.service;

import com.wfp.common.exception.NotFoundException;
import com.wfp.security.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeploymentService {

    private final RepositoryService repositoryService;

    public Deployment deploy(String name, String category, String bpmnXml) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return repositoryService.createDeployment()
                .name(name)
                .category(category)
                .addString(name + ".bpmn20.xml", bpmnXml)
                .tenantId(tenantId)
                .deploy();
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
        if (pd == null) throw new NotFoundException("ProcessDefinition", processDefinitionId);
        return pd;
    }

    public void deleteDeployment(String deploymentId) {
        repositoryService.deleteDeployment(deploymentId, true);
    }
}
