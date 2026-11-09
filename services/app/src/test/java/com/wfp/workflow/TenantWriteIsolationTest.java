package com.wfp.workflow;

import org.flowable.engine.RepositoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantWriteIsolationTest {

    private static final String BPMN = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                     targetNamespace="http://wfp.com/test">
          <process id="writeIsolation" name="Write Isolation" isExecutable="true">
            <startEvent id="start"/>
            <userTask id="review" name="Review"/>
            <endEvent id="end"/>
            <sequenceFlow sourceRef="start" targetRef="review"/>
            <sequenceFlow sourceRef="review" targetRef="end"/>
          </process>
        </definitions>
        """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepositoryService repositoryService;

    private String deploymentId;

    @BeforeEach
    void deployInTenantA() {
        deploymentId = repositoryService.createDeployment()
                .addString("write-isolation.bpmn20.xml", BPMN)
                .tenantId("tenant-a")
                .deploy()
                .getId();
    }

    @AfterEach
    void deleteDeployment() {
        if (deploymentExists()) {
            repositoryService.deleteDeployment(deploymentId, true);
        }
    }

    @Test
    void otherTenantCannotDeleteDeployment() throws Exception {
        deleteDeploymentAs("tenant-b").andExpect(status().isNotFound());

        assertThat(deploymentExists()).isTrue();
    }

    @Test
    void ownTenantDeletesDeployment() throws Exception {
        deleteDeploymentAs("tenant-a").andExpect(status().isNoContent());

        assertThat(deploymentExists()).isFalse();
    }

    private ResultActions deleteDeploymentAs(String tenantId) throws Exception {
        return mockMvc.perform(delete("/api/workflow/deployments/{id}", deploymentId)
                .with(jwt().jwt(token -> token.claim("preferred_username", "user-" + tenantId)
                        .claim("tenant_id", tenantId)).authorities(TestRoles.ADMIN)));
    }

    private boolean deploymentExists() {
        return repositoryService.createDeploymentQuery().deploymentId(deploymentId).count() > 0;
    }
}
