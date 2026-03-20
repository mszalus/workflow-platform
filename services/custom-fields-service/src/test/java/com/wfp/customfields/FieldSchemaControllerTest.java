package com.wfp.customfields;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfp.customfields.dto.CreateFieldSchemaRequest;
import com.wfp.customfields.entity.FieldType;
import com.wfp.security.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FieldSchemaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenantId("tenant-test");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateFieldSchema() throws Exception {
        CreateFieldSchemaRequest request = new CreateFieldSchemaRequest();
        request.setProcessDefinitionKey("testProcess");
        request.setFieldKey("customerName");
        request.setLabel("Customer Name");
        request.setFieldType(FieldType.TEXT);
        request.setRequired(true);

        mockMvc.perform(post("/api/schemas")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "admin")
                                .claim("tenant_id", "tenant-test")))
                        .header("X-Tenant-Id", "tenant-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fieldKey").value("customerName"))
                .andExpect(jsonPath("$.fieldType").value("TEXT"));
    }

    @Test
    void shouldListSchemasByProcessDefinition() throws Exception {
        mockMvc.perform(get("/api/schemas")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "admin")
                                .claim("tenant_id", "tenant-test")))
                        .header("X-Tenant-Id", "tenant-test")
                        .param("processDefinitionKey", "testProcess"))
                .andExpect(status().isOk());
    }
}
