package com.wfp.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfp.security.context.TenantContext;
import com.wfp.workflow.dto.DeployProcessRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeploymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String SIMPLE_BPMN = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                     targetNamespace="http://wfp.com/test">
          <process id="testProcess" name="Test Process" isExecutable="true">
            <startEvent id="start"/>
            <userTask id="task1" name="Review Task"/>
            <endEvent id="end"/>
            <sequenceFlow sourceRef="start" targetRef="task1"/>
            <sequenceFlow sourceRef="task1" targetRef="end"/>
          </process>
        </definitions>
        """;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenantId("tenant-test");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldDeployBpmnProcess() throws Exception {
        DeployProcessRequest request = new DeployProcessRequest();
        request.setName("Test Process");
        request.setCategory("test");
        request.setBpmnXml(SIMPLE_BPMN);

        mockMvc.perform(post("/api/deployments")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "admin")
                                .claim("tenant_id", "tenant-test")))
                        .header("X-Tenant-Id", "tenant-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deploymentId").exists());
    }

    @Test
    void shouldListProcessDefinitions() throws Exception {
        mockMvc.perform(get("/api/deployments")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "admin")
                                .claim("tenant_id", "tenant-test")))
                        .header("X-Tenant-Id", "tenant-test"))
                .andExpect(status().isOk());
    }
}
