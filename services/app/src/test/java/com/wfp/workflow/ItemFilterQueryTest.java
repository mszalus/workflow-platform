package com.wfp.workflow;

import com.wfp.security.context.TenantContext;
import com.wfp.workflow.engine.Samples;
import com.wfp.workflow.engine.flowable.DeploymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("pgtest")
class ItemFilterQueryTest {

    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static {
        postgres.start();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeploymentService deploymentService;

    private final String tenant = "tenant-" + UUID.randomUUID();

    @BeforeEach
    void seedItems() throws Exception {
        TenantContext.runAs(tenant, () -> deploymentService.deploy("simple", null, Samples.xml("valid/simple")));
        perform(post("/api/projects"), "admin", """
                {"key":"PROJ","name":"Platform","itemTypes":[{"name":"Task","workflowKey":"simple"}]}
                """).andExpect(status().isCreated());
        declareOptionalTextField("severity");
        declareOptionalTextField("due");

        create("Fix login", "alice", "HIGH", "{\"severity\":\"High\",\"due\":\"2026-11-01\"}");
        create("Add export", "bob", "LOW", "{\"severity\":\"Low\",\"due\":\"2026-12-24\"}");
        create("Crash on save", "alice", "MEDIUM", "{\"severity\":\"Critical\",\"due\":\"2026-10-30\"}");
    }

    @Test
    void filtersOnACustomFieldInsideTheJsonbColumn() throws Exception {
        list("filter=cf.severity:eq:Critical")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Crash on save"));
    }

    @Test
    void filtersOnSeveralCustomFieldValuesAtOnce() throws Exception {
        list("filter=cf.severity:in:High,Critical").andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void keepsACommaInsideASingleValue() throws Exception {
        create("Comma in value", "alice", "LOW", "{\"severity\":\"High,urgent\"}");

        list("filter=cf.severity:eq:High,urgent")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Comma in value"));
    }

    @Test
    void keepsAnEscapedCommaInsideAnInList() throws Exception {
        create("Comma in option", "alice", "LOW", "{\"severity\":\"Yes, please\"}");

        list("filter=cf.severity:in:Yes\\, please,Critical").andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void treatsSqlInAFilterAsPlainData() throws Exception {
        list("filter=cf.x') OR 1=1 --:eq:a").andExpect(jsonPath("$.totalElements").value(0));
        list("filter=cf.severity:eq:High' OR '1'='1").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void comparesCustomFieldsAsText() throws Exception {
        list("filter=cf.due:lte:2026-11-01").andExpect(jsonPath("$.totalElements").value(2));
        list("filter=cf.due:gte:2026-12-01").andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void excludesItemsWithoutTheCustomFieldOnNotEqual() throws Exception {
        create("No severity", "alice", "LOW", "{}");

        list("filter=cf.severity:ne:Low").andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void combinesAFixedFieldAndACustomFieldInOneQuery() throws Exception {
        list("filter=assignee:eq:alice&filter=cf.severity:eq:High")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Fix login"));
    }

    @Test
    void resolvesMeToTheCaller() throws Exception {
        list("filter=assignee:eq:me", "alice").andExpect(jsonPath("$.totalElements").value(2));
        list("filter=assignee:eq:me", "bob").andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void filtersOnAFixedEnumField() throws Exception {
        list("filter=priority:in:HIGH,LOW").andExpect(jsonPath("$.totalElements").value(2));
        list("filter=statusCategory:eq:OPEN").andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void sortsByAFixedFieldAndPages() throws Exception {
        list("sort=title,asc&size=2")
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Add export"))
                .andExpect(jsonPath("$.totalElements").value(3));
        list("sort=title,desc").andExpect(jsonPath("$.content[0].title").value("Fix login"));
    }

    @Test
    void refusesAFilterItCannotUnderstand() throws Exception {
        list("filter=nonsense").andExpect(status().isBadRequest());
        list("filter=assignee:like:alice").andExpect(status().isBadRequest());
        list("filter=unknownField:eq:x").andExpect(status().isBadRequest());
        list("filter=priority:eq:URGENT").andExpect(status().isBadRequest());
        list("sort=description,asc").andExpect(status().isBadRequest());
    }

    @Test
    void keepsAnotherTenantsItemsOutOfTheResult() throws Exception {
        mockMvc.perform(get("/api/items?filter=cf.severity:eq:High")
                        .with(jwt().jwt(token -> token.claim("preferred_username", "mallory")
                                .claim("organization", List.of("tenant-other"))).authorities(TestRoles.USER)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private void declareOptionalTextField(String key) throws Exception {
        perform(post("/api/fields/schemas"), "admin", """
                {"project":"PROJ","fieldKey":"%s","label":"%s","fieldType":"TEXT"}
                """.formatted(key, key)).andExpect(status().isCreated());
    }

    private void create(String title, String assignee, String priority, String fields) throws Exception {
        perform(post("/api/items"), "alice", """
                {"project":"PROJ","type":"Task","title":"%s","assignee":"%s","priority":"%s","fields":%s}
                """.formatted(title, assignee, priority, fields)).andExpect(status().isCreated());
    }

    private ResultActions list(String query) throws Exception {
        return list(query, "alice");
    }

    private ResultActions list(String query, String user) throws Exception {
        return perform(get("/api/items?" + query), user, null);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String user, String body) throws Exception {
        request.with(jwt().jwt(token -> token.claim("preferred_username", user)
                .claim("organization", List.of(tenant)))
                .authorities("admin".equals(user) ? TestRoles.ADMIN : TestRoles.USER));
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }
}
