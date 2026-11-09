package com.wfp.workflow;

import com.wfp.security.context.TenantContext;
import com.wfp.workflow.engine.Samples;
import com.wfp.workflow.engine.flowable.DeploymentService;
import com.wfp.workflow.entity.AuditEntry;
import com.wfp.workflow.entity.ItemTransition;
import com.wfp.workflow.repository.AuditEntryRepository;
import com.wfp.workflow.repository.ItemRepository;
import com.wfp.workflow.repository.ItemTransitionRepository;
import com.wfp.workflow.repository.NotificationRepository;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkflowUpgradeTest {

    private static final String FIRST_VERSION = workflow("upgradeFlow", """
            <userTask id="triage" name="Triage" wfp:statusCategory="OPEN"/>
            <userTask id="doing" name="Doing" wfp:statusCategory="IN_PROGRESS"/>
            <userTask id="review" name="Review" wfp:statusCategory="IN_PROGRESS"/>
            <userTask id="done" name="Done" wfp:statusCategory="DONE"/>
            <endEvent id="closed" name="Closed"/>
            <sequenceFlow id="enter" sourceRef="start" targetRef="triage"/>
            <sequenceFlow id="toDoing" name="Start" sourceRef="triage" targetRef="doing"/>
            <sequenceFlow id="toReview" name="Submit" sourceRef="doing" targetRef="review"/>
            <sequenceFlow id="toDone" name="Approve" sourceRef="review" targetRef="done"/>
            <sequenceFlow id="close" name="Close" sourceRef="done" targetRef="closed"/>
            """);

    private static final String SECOND_VERSION = workflow("upgradeFlow", """
            <userTask id="intake" name="Intake" wfp:statusCategory="OPEN"/>
            <userTask id="doing" name="Doing" wfp:statusCategory="IN_PROGRESS"/>
            <userTask id="done" name="Done" wfp:statusCategory="DONE"/>
            <endEvent id="closed" name="Closed"/>
            <sequenceFlow id="enter" sourceRef="start" targetRef="intake"/>
            <sequenceFlow id="toDoing" name="Start" sourceRef="intake" targetRef="doing"/>
            <sequenceFlow id="finish" name="Finish" sourceRef="doing" targetRef="done"/>
            <sequenceFlow id="close" name="Close" sourceRef="done" targetRef="closed"/>
            """);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeploymentService deploymentService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemTransitionRepository transitionRepository;

    @Autowired
    private AuditEntryRepository auditEntryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private final String tenant = "tenant-" + UUID.randomUUID();
    private final String otherTenant = "tenant-" + UUID.randomUUID();

    @BeforeEach
    void createItems() throws Exception {
        deploy(tenant, FIRST_VERSION);
        createProject(tenant, "upgradeFlow");
        createItem(tenant, "Stays in Doing");
        createItem(tenant, "In Review");
        createItem(tenant, "In Triage");
        createItem(tenant, "Closed already");
        transition("PROJ-1", "toDoing");
        transition("PROJ-2", "toDoing");
        transition("PROJ-2", "toReview");
        transition("PROJ-4", "toDoing");
        transition("PROJ-4", "toReview");
        transition("PROJ-4", "toDone");
        transition("PROJ-4", "close");
    }

    @AfterEach
    void deleteDeployments() {
        for (String owner : List.of(tenant, otherTenant)) {
            repositoryService.createDeploymentQuery().deploymentTenantId(owner).list()
                    .forEach(deployment -> repositoryService.deleteDeployment(deployment.getId(), true));
        }
    }

    @Test
    void aDryRunCountsEachOutcomeAndMovesNothing() throws Exception {
        String runBefore = item("PROJ-1", "$.statusName");

        preview(SECOND_VERSION)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowKey").value("upgradeFlow"))
                .andExpect(jsonPath("$.openItems").value(3))
                .andExpect(jsonPath("$.sameStatus").value(1))
                .andExpect(jsonPath("$.walkedBack").value(1))
                .andExpect(jsonPath("$.reset").value(1));

        assertThat(item("PROJ-1", "$.statusName")).isEqualTo(runBefore);
        assertThat(item("PROJ-3", "$.statusName")).isEqualTo("Triage");
    }

    @Test
    void movesOpenItemsToTheNewVersionByOutcome() throws Exception {
        deploy(tenant, SECOND_VERSION);

        upgrade("upgradeFlow")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openItems").value(3))
                .andExpect(jsonPath("$.sameStatus").value(1))
                .andExpect(jsonPath("$.walkedBack").value(1))
                .andExpect(jsonPath("$.reset").value(1))
                .andExpect(jsonPath("$.failed").value(0));

        assertThat(item("PROJ-1", "$.statusName")).isEqualTo("Doing");
        assertThat(item("PROJ-2", "$.statusName")).isEqualTo("Doing");
        assertThat(item("PROJ-3", "$.statusName")).isEqualTo("Intake");
        assertThat(item("PROJ-3", "$.statusCategory")).isEqualTo("OPEN");
    }

    @Test
    void aMovedItemKeepsWorkingOnTheNewVersion() throws Exception {
        String oldRun = runOf("PROJ-1");
        deploy(tenant, SECOND_VERSION);
        upgrade("upgradeFlow").andExpect(status().isOk());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(oldRun).count()).isZero();
        assertThat(runOf("PROJ-1")).isNotEqualTo(oldRun);
        perform(get("/api/items/PROJ-1"), tenant, "alice", null)
                .andExpect(jsonPath("$.transitions[*].id", contains("finish")));

        transition("PROJ-1", "finish");

        assertThat(item("PROJ-1", "$.statusName")).isEqualTo("Done");
    }

    @Test
    void recordsTheMoveWithoutNotifyingAnyone() throws Exception {
        deploy(tenant, SECOND_VERSION);
        int notificationsBefore = notifications();

        upgrade("upgradeFlow").andExpect(status().isOk());

        ItemTransition last = asTenant(() -> {
            UUID id = itemRepository.findByKey("PROJ-3").orElseThrow().getId();
            return transitionRepository.findByItemIdOrderByTransitionedAtAsc(id).getLast();
        });
        assertThat(last.getReason()).isEqualTo("WORKFLOW_UPGRADE");
        assertThat(last.getFromStatus()).isEqualTo("triage");
        assertThat(last.getToStatus()).isEqualTo("intake");
        assertThat(last.getActor()).isEqualTo("admin");

        List<AuditEntry> audit = asTenant(() -> auditEntryRepository.findAll()).stream()
                .filter(entry -> "item.workflow.upgraded".equals(entry.getEventType()))
                .toList();
        assertThat(audit).extracting(AuditEntry::getEntityId)
                .containsExactlyInAnyOrder("PROJ-1", "PROJ-2", "PROJ-3");
        assertThat(notifications()).isEqualTo(notificationsBefore);
        perform(get("/api/items/PROJ-3/history"), tenant, "alice", null)
                .andExpect(jsonPath("$[0].kind").value("STATUS"))
                .andExpect(jsonPath("$[0].from").value("Triage"))
                .andExpect(jsonPath("$[0].to").value("Intake"))
                .andExpect(jsonPath("$[0].reason").value("WORKFLOW_UPGRADE"));
    }

    @Test
    void leavesClosedItemsAlone() throws Exception {
        String versionBefore = versionOf("PROJ-4");
        deploy(tenant, SECOND_VERSION);

        upgrade("upgradeFlow").andExpect(status().isOk());

        assertThat(versionOf("PROJ-4")).isEqualTo(versionBefore);
    }

    @Test
    void movingAgainFindsNothingToMove() throws Exception {
        deploy(tenant, SECOND_VERSION);
        upgrade("upgradeFlow").andExpect(status().isOk());

        upgrade("upgradeFlow").andExpect(jsonPath("$.openItems").value(0));
    }

    @Test
    void movesAnItemIntoAStatusSubprocessWithItsStepsOpen() throws Exception {
        deploy(tenant, Samples.xml("valid/approval-subprocess"));
        perform(post("/api/projects"), tenant, "admin", """
                {"key":"APPR","name":"Approvals","itemTypes":[{"name":"Request","workflowKey":"approval"}]}
                """).andExpect(status().isCreated());
        perform(post("/api/items"), tenant, "alice", """
                {"project":"APPR","type":"Request","title":"New supplier"}
                """).andExpect(status().isCreated());
        transition("APPR-1", "submit");
        deploy(tenant, Samples.xml("valid/approval-subprocess"));

        upgrade("approval")
                .andExpect(jsonPath("$.sameStatus").value(1))
                .andExpect(jsonPath("$.failed").value(0));

        assertThat(item("APPR-1", "$.statusName")).isEqualTo("In Approval");
        perform(get("/api/items/APPR-1"), tenant, "alice", null)
                .andExpect(jsonPath("$.openSteps.length()").value(2));
    }

    @Test
    void doesNotTouchAnotherTenantsItems() throws Exception {
        deploy(otherTenant, FIRST_VERSION);
        createProject(otherTenant, "upgradeFlow");
        createItem(otherTenant, "Elsewhere");
        String versionBefore = otherTenantVersionOf("PROJ-1");
        deploy(tenant, SECOND_VERSION);

        upgrade("upgradeFlow").andExpect(jsonPath("$.openItems").value(3));

        assertThat(otherTenantVersionOf("PROJ-1")).isEqualTo(versionBefore);
    }

    @Test
    void refusesAnUnknownWorkflowOrOneThatIsNotATrackerWorkflow() throws Exception {
        upgrade("noSuchWorkflow").andExpect(status().isNotFound());

        preview(FIRST_VERSION.replace(" wfp:statusCategory=\"OPEN\"", "")
                .replace(" wfp:statusCategory=\"IN_PROGRESS\"", "")
                .replace(" wfp:statusCategory=\"DONE\"", ""))
                .andExpect(status().isBadRequest());
    }

    private static String workflow(String key, String body) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:wfp="http://wfp.com/schema/tracker"
                             targetNamespace="http://wfp.com/test">
                  <process id="%s" isExecutable="true">
                    <startEvent id="start"/>
                    %s
                  </process>
                </definitions>
                """.formatted(key, body);
    }

    private void deploy(String owner, String xml) {
        TenantContext.runAs(owner, () -> deploymentService.deploy("workflow", null, xml));
    }

    private void createProject(String owner, String workflowKey) throws Exception {
        perform(post("/api/projects"), owner, "admin", """
                {"key":"PROJ","name":"Platform","itemTypes":[{"name":"Task","workflowKey":"%s"}]}
                """.formatted(workflowKey)).andExpect(status().isCreated());
    }

    private void createItem(String owner, String title) throws Exception {
        perform(post("/api/items"), owner, "alice", """
                {"project":"PROJ","type":"Task","title":"%s","assignee":"bob"}
                """.formatted(title)).andExpect(status().isCreated());
    }

    private void transition(String key, String transitionId) throws Exception {
        perform(post("/api/items/{key}/transitions", key), tenant, "alice", """
                {"transitionId":"%s"}
                """.formatted(transitionId)).andExpect(status().isOk());
    }

    private ResultActions preview(String xml) throws Exception {
        return perform(post("/api/workflow/workflows/upgrade-preview"), tenant, "admin",
                objectMapper.writeValueAsString(Map.of("bpmnXml", xml)));
    }

    private ResultActions upgrade(String workflowKey) throws Exception {
        return perform(post("/api/workflow/workflows/{key}/upgrade", workflowKey), tenant, "admin", null);
    }

    private String item(String key, String path) throws Exception {
        String body = perform(get("/api/items/{key}", key), tenant, "alice", null)
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, path);
    }

    private String runOf(String key) {
        return asTenant(() -> itemRepository.findByKey(key).orElseThrow().getRunId());
    }

    private String versionOf(String key) {
        return asTenant(() -> itemRepository.findByKey(key).orElseThrow().getWorkflowVersionId());
    }

    private String otherTenantVersionOf(String key) {
        return asTenant(otherTenant, () -> itemRepository.findByKey(key).orElseThrow().getWorkflowVersionId());
    }

    private int notifications() {
        return asTenant(() -> notificationRepository.findAll()).size();
    }

    private <T> T asTenant(java.util.function.Supplier<T> query) {
        return asTenant(tenant, query);
    }

    private <T> T asTenant(String owner, java.util.function.Supplier<T> query) {
        Object[] result = new Object[1];
        TenantContext.runAs(owner, () -> result[0] = query.get());
        @SuppressWarnings("unchecked")
        T typed = (T) result[0];
        return typed;
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String owner, String user, String body)
            throws Exception {
        request.with(jwt().jwt(token -> token.claim("preferred_username", user).claim("organization", List.of(owner)))
                .authorities("admin".equals(user) ? TestRoles.ADMIN : TestRoles.USER));
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }
}
