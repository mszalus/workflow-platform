package com.wfp.workflow;

import com.wfp.security.context.TenantContext;
import com.wfp.workflow.engine.Samples;
import com.wfp.workflow.engine.flowable.DeploymentService;
import com.wfp.workflow.entity.AuditEntry;
import com.wfp.workflow.entity.ItemTransition;
import com.wfp.workflow.entity.Notification;
import com.wfp.workflow.repository.AuditEntryRepository;
import com.wfp.workflow.repository.ItemRepository;
import com.wfp.workflow.repository.ItemTransitionRepository;
import com.wfp.workflow.repository.NotificationRepository;
import org.flowable.engine.RepositoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ItemControllerTest {

    private static final String ITEM = """
        {"project":"PROJ","type":"Task","title":"Fix login","assignee":"bob","fields":{"severity":"High"}}
        """;

    private static final String APPROVAL_ITEM = """
        {"project":"PROJ","type":"Request","title":"New supplier","assignee":"bob"}
        """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeploymentService deploymentService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemTransitionRepository transitionRepository;

    @Autowired
    private AuditEntryRepository auditEntryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private final String tenant = "tenant-" + UUID.randomUUID();

    @BeforeEach
    void createProject() throws Exception {
        TenantContext.runAs(tenant, () -> {
            deploymentService.deploy("simple", null, Samples.xml("valid/simple"));
            deploymentService.deploy("approval", null, Samples.xml("valid/approval-subprocess"));
        });
        perform(post("/api/projects"), "admin", """
                {"key":"PROJ","name":"Platform","itemTypes":[{"name":"Task","workflowKey":"simple"},
                                                             {"name":"Request","workflowKey":"approval"}]}
                """).andExpect(status().isCreated());
        createField("""
                {"project":"PROJ","itemTypes":["Task"],"fieldKey":"severity","label":"Severity","fieldType":"DROPDOWN",
                 "required":true,"options":[{"label":"High","value":"High"},{"label":"Low","value":"Low"}]}
                """).andExpect(status().isCreated());
    }

    @AfterEach
    void deleteDeployments() {
        repositoryService.createDeploymentQuery().deploymentTenantId(tenant).list()
                .forEach(deployment -> repositoryService.deleteDeployment(deployment.getId(), true));
    }

    @Test
    void createsItemsWithProjectKeysInTheInitialStatus() throws Exception {
        perform(post("/api/items"), "alice", ITEM)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("PROJ-1"))
                .andExpect(jsonPath("$.reporter").value("alice"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.statusName").value("Open"))
                .andExpect(jsonPath("$.statusCategory").value("OPEN"))
                .andExpect(jsonPath("$.fields.severity").value("High"))
                .andExpect(jsonPath("$.workflowVersionId").isNotEmpty())
                .andExpect(jsonPath("$.statuses[?(@.id=='doing')].category").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.transitions[0].id").value("startWork"));

        perform(post("/api/items"), "alice", ITEM).andExpect(jsonPath("$.key").value("PROJ-2"));
        perform(get("/api/items?project=PROJ"), "alice", null).andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void transitionsToClosedAndRecordsEveryStep() throws Exception {
        perform(post("/api/items"), "alice", ITEM).andExpect(status().isCreated());

        transition("startWork").andExpect(jsonPath("$.statusName").value("Doing"));
        transition("finish").andExpect(jsonPath("$.statusCategory").value("DONE"));
        transition("close")
                .andExpect(jsonPath("$.statusName").value("Closed"))
                .andExpect(jsonPath("$.statusCategory").value("DONE"))
                .andExpect(jsonPath("$.transitions").isEmpty());
        transition("close").andExpect(status().isBadRequest());

        assertThat(asTenant(() -> transitionRepository.findByItemIdOrderByTransitionedAtAsc(itemId("PROJ-1"))))
                .extracting(ItemTransition::getFromStatus, ItemTransition::getToStatus,
                        ItemTransition::getTransitionId, ItemTransition::getActor)
                .containsExactly(
                        tuple(null, "open", null, "alice"),
                        tuple("open", "doing", "startWork", "alice"),
                        tuple("doing", "done", "finish", "alice"),
                        tuple("done", "END", "close", "alice"));
        assertThat(auditEvents("PROJ-1"))
                .containsExactly("item.created", "item.transitioned", "item.transitioned", "item.transitioned");
        assertThat(notificationTitles("bob")).containsExactlyInAnyOrder("PROJ-1 assigned to you",
                "PROJ-1 moved to Doing", "PROJ-1 moved to Done", "PROJ-1 moved to Closed");
        assertThat(notificationTitles("alice")).isEmpty();
    }

    @Test
    void movesAnItemThroughAStatusSubprocessByCompletingItsSteps() throws Exception {
        perform(post("/api/items"), "alice", APPROVAL_ITEM).andExpect(status().isCreated());
        transition("submit")
                .andExpect(jsonPath("$.statusName").value("In Approval"))
                .andExpect(jsonPath("$.transitions").isEmpty())
                .andExpect(jsonPath("$.openSteps.length()").value(2))
                .andExpect(jsonPath("$.openSteps[0].id").value("legal"))
                .andExpect(jsonPath("$.openSteps[0].name").value("Legal approval"))
                .andExpect(jsonPath("$.openSteps[1].id").value("finance"));

        completeStep("legal")
                .andExpect(jsonPath("$.statusName").value("In Approval"))
                .andExpect(jsonPath("$.openSteps.length()").value(1))
                .andExpect(jsonPath("$.openSteps[0].id").value("finance"));
        completeStep("finance")
                .andExpect(jsonPath("$.statusName").value("Approved"))
                .andExpect(jsonPath("$.openSteps").isEmpty());

        assertThat(asTenant(() -> transitionRepository.findByItemIdOrderByTransitionedAtAsc(itemId("PROJ-1"))))
                .extracting(ItemTransition::getFromStatus, ItemTransition::getToStatus, ItemTransition::getTransitionId)
                .containsExactly(
                        tuple(null, "draft", null),
                        tuple("draft", "inApproval", "submit"),
                        tuple("inApproval", "approved", null));
        assertThat(auditEvents("PROJ-1")).containsExactly("item.created", "item.transitioned",
                "item.step.completed", "item.transitioned", "item.step.completed");
    }

    @Test
    void refusesToLeaveAStatusSubprocessByTransitionOrToRepeatAStep() throws Exception {
        perform(post("/api/items"), "alice", APPROVAL_ITEM).andExpect(status().isCreated());
        transition("submit").andExpect(status().isOk());

        transition("complete").andExpect(status().isBadRequest());
        completeStep("draft").andExpect(status().isBadRequest());
        completeStep("legal").andExpect(status().isOk());
        completeStep("legal").andExpect(status().isBadRequest());
    }

    @Test
    void refusesATransitionTheStatusDoesNotOffer() throws Exception {
        perform(post("/api/items"), "alice", ITEM).andExpect(status().isCreated());

        transition("close").andExpect(status().isBadRequest());
    }

    @Test
    void updateRecordsTheFieldDiffAndNotifiesTheNewAssigneeOnce() throws Exception {
        perform(post("/api/items"), "alice", ITEM).andExpect(status().isCreated());

        perform(patch("/api/items/PROJ-1"), "alice", """
                {"priority":"HIGH","assignee":"carol","fields":{"severity":"Low"}}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.fields.severity").value("Low"));
        perform(patch("/api/items/PROJ-1"), "alice", "{\"assignee\":\"carol\"}").andExpect(status().isOk());

        AuditEntry update = asTenant(() -> auditEntryRepository.findAll()).stream()
                .filter(entry -> entry.getEventType().equals("item.updated"))
                .findFirst().orElseThrow();
        assertThat(update.getDetails()).contains(
                "{\"field\":\"priority\",\"from\":\"MEDIUM\",\"to\":\"HIGH\"}",
                "{\"field\":\"assignee\",\"from\":\"bob\",\"to\":\"carol\"}",
                "{\"field\":\"fields.severity\",\"from\":\"High\",\"to\":\"Low\"}");
        assertThat(auditEvents("PROJ-1")).containsExactly("item.created", "item.updated");
        assertThat(notificationTitles("carol")).containsExactly("PROJ-1 assigned to you");
    }

    @Test
    void refusesChangesThatAreNotTextOrTooLong() throws Exception {
        perform(post("/api/items"), "alice", ITEM).andExpect(status().isCreated());

        perform(patch("/api/items/PROJ-1"), "alice", "{\"title\":42}").andExpect(status().isBadRequest());
        perform(patch("/api/items/PROJ-1"), "alice", "{\"title\":\"" + "x".repeat(256) + "\"}")
                .andExpect(status().isBadRequest());
        perform(post("/api/items"), "alice", ITEM.replace("Fix login", "x".repeat(256)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refusesToCreateAnItemOnANewVersionThatIsNotATrackerWorkflow() throws Exception {
        String brokenVersion = Samples.xml("valid/simple").replace(" wfp:statusCategory=\"DONE\"", "");
        deployUnchecked("simple", brokenVersion);

        perform(post("/api/items"), "alice", ITEM).andExpect(status().isBadRequest());
    }

    @Test
    void validatesCustomFields() throws Exception {
        perform(post("/api/items"), "alice", ITEM.replace(",\"fields\":{\"severity\":\"High\"}", ""))
                .andExpect(status().isBadRequest());
        perform(post("/api/items"), "alice", ITEM.replace("\"High\"", "\"Urgent\""))
                .andExpect(status().isBadRequest());
        perform(post("/api/items"), "alice", ITEM.replace("\"severity\"", "\"colour\""))
                .andExpect(status().isBadRequest());
        perform(post("/api/items"), "alice", ITEM).andExpect(jsonPath("$.key").value("PROJ-1"));
    }

    @Test
    void refusesAProjectWhoseWorkflowIsNotATrackerWorkflow() throws Exception {
        String plainBpmn = Samples.xml("valid/simple").replace(" wfp:statusCategory=\"OPEN\"", "")
                .replace("id=\"simple\"", "id=\"plain\"");
        deployUnchecked("plain", plainBpmn);

        perform(post("/api/projects"), "admin", """
                {"key":"PLAIN","name":"Plain","itemTypes":[{"name":"Task","workflowKey":"plain"}]}
                """).andExpect(status().isBadRequest());
        perform(post("/api/projects"), "admin", """
                {"key":"proj","name":"Lower","itemTypes":[{"name":"Task","workflowKey":"simple"}]}
                """).andExpect(status().isBadRequest());
        perform(post("/api/projects"), "admin", """
                {"key":"PROJ","name":"Again","itemTypes":[{"name":"Task","workflowKey":"simple"}]}
                """).andExpect(status().isBadRequest());
    }

    @Test
    void commentsRoundTripOnAnItemOldestFirst() throws Exception {
        perform(post("/api/items"), "alice", ITEM).andExpect(status().isCreated());

        perform(post("/api/items/PROJ-1/comments"), "carol", "{\"content\":\"First\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("carol"));
        perform(post("/api/items/PROJ-1/comments"), "alice", "{\"content\":\"Second\"}")
                .andExpect(status().isCreated());

        perform(get("/api/items/PROJ-1/comments"), "bob", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("First"))
                .andExpect(jsonPath("$[1].content").value("Second"));
        assertThat(auditEvents("PROJ-1")).containsExactly("item.created", "item.commented", "item.commented");
    }

    @Test
    void aCommentNotifiesTheReporterAndAssigneeExceptItsAuthor() throws Exception {
        perform(post("/api/items"), "alice", ITEM).andExpect(status().isCreated());

        perform(post("/api/items/PROJ-1/comments"), "carol", "{\"content\":\"Hello\"}")
                .andExpect(status().isCreated());
        perform(post("/api/items/PROJ-1/comments"), "bob", "{\"content\":\"Hi\"}")
                .andExpect(status().isCreated());

        assertThat(notificationTitles("alice"))
                .containsExactlyInAnyOrder("carol commented on PROJ-1", "bob commented on PROJ-1");
        assertThat(notificationTitles("bob"))
                .contains("carol commented on PROJ-1")
                .doesNotContain("bob commented on PROJ-1");
        assertThat(notificationTitles("carol")).isEmpty();
    }

    @Test
    void refusesAnEmptyCommentOrACommentOnAnUnknownItem() throws Exception {
        perform(post("/api/items"), "alice", ITEM).andExpect(status().isCreated());

        perform(post("/api/items/PROJ-1/comments"), "alice", "{\"content\":\" \"}")
                .andExpect(status().isBadRequest());
        perform(post("/api/items/PROJ-99/comments"), "alice", "{\"content\":\"Hello\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    void renamesAProjectAndAddsAnItemType() throws Exception {
        updateProject("""
                {"name":"Renamed","itemTypes":[{"name":"Task","workflowKey":"simple"},
                                               {"name":"Request","workflowKey":"approval"},
                                               {"name":"Bug","workflowKey":"simple"}]}
                """).andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("PROJ"))
                .andExpect(jsonPath("$.name").value("Renamed"));

        perform(get("/api/projects"), "alice", null)
                .andExpect(jsonPath("$[0].itemTypes[*].name", containsInAnyOrder("Task", "Request", "Bug")));
        perform(post("/api/items"), "alice", """
                {"project":"PROJ","type":"Bug","title":"Works on the new type"}
                """).andExpect(status().isCreated());
        assertThat(auditEvents("PROJ")).contains("project.updated");
    }

    @Test
    void removesAnItemTypeNothingUses() throws Exception {
        updateProject("""
                {"name":"Platform","itemTypes":[{"name":"Task","workflowKey":"simple"}]}
                """).andExpect(status().isOk());

        perform(get("/api/projects"), "alice", null)
                .andExpect(jsonPath("$[0].itemTypes[*].name", containsInAnyOrder("Task")));
    }

    @Test
    void refusesToRemoveAnItemTypeThatHasItems() throws Exception {
        perform(post("/api/items"), "alice", APPROVAL_ITEM).andExpect(status().isCreated());

        updateProject("""
                {"name":"Platform","itemTypes":[{"name":"Task","workflowKey":"simple"}]}
                """).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Item type Request still has items"));

        perform(get("/api/projects"), "alice", null)
                .andExpect(jsonPath("$[0].itemTypes[*].name", containsInAnyOrder("Task", "Request")));
    }

    @Test
    void refusesToRemoveAnItemTypeThatHasFieldDefinitions() throws Exception {
        updateProject("""
                {"name":"Platform","itemTypes":[{"name":"Request","workflowKey":"approval"}]}
                """).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Item type Task still has field definitions"));
    }

    @Test
    void refusesInvalidProjectChanges() throws Exception {
        updateProject("""
                {"name":"Platform","itemTypes":[{"name":"Task","workflowKey":"approval"},
                                               {"name":"Request","workflowKey":"approval"}]}
                """).andExpect(status().isBadRequest());
        updateProject("""
                {"name":"Platform","itemTypes":[{"name":"Task","workflowKey":"simple"},
                                               {"name":"Task","workflowKey":"simple"}]}
                """).andExpect(status().isBadRequest());
        updateProject("""
                {"name":" ","itemTypes":[{"name":"Task","workflowKey":"simple"}]}
                """).andExpect(status().isBadRequest());
        updateProject("""
                {"name":"Platform","itemTypes":[]}
                """).andExpect(status().isBadRequest());
    }

    @Test
    void anotherTenantCannotEditAProject() throws Exception {
        mockMvc.perform(put("/api/projects/PROJ").with(jwt().jwt(token -> token.claim("preferred_username", "mallory")
                                .claim("tenant_id", "tenant-other")).authorities(TestRoles.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Stolen","itemTypes":[{"name":"Task","workflowKey":"simple"}]}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void resolvesFieldDefinitionsByProjectAndItemType() throws Exception {
        createField(textField("[]", "component")).andExpect(status().isCreated());
        createField(textField("[\"Request\"]", "budget")).andExpect(status().isCreated());

        perform(get("/api/fields/schemas?project=PROJ&type=Task"), "alice", null)
                .andExpect(jsonPath("$[*].fieldKey", containsInAnyOrder("severity", "component")));
        perform(get("/api/fields/schemas?project=PROJ&type=Request"), "alice", null)
                .andExpect(jsonPath("$[*].fieldKey", containsInAnyOrder("component", "budget")));
        perform(get("/api/fields/schemas?project=PROJ"), "alice", null)
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.fieldKey=='severity')].itemTypes[0]").value("Task"));
    }

    @Test
    void validatesAnItemAgainstTheDefinitionsOfItsItemType() throws Exception {
        perform(post("/api/items"), "alice", APPROVAL_ITEM).andExpect(status().isCreated());

        perform(post("/api/items"), "alice", """
                {"project":"PROJ","type":"Request","title":"New supplier","fields":{"severity":"High"}}
                """).andExpect(status().isBadRequest());
    }

    @Test
    void refusesAFieldKeyThatOverlapsAnotherDefinitionsItemTypes() throws Exception {
        createField(textField("[\"Task\"]", "severity")).andExpect(status().isBadRequest());
        createField(textField("[]", "severity")).andExpect(status().isBadRequest());

        createField(textField("[\"Request\"]", "severity")).andExpect(status().isCreated());
    }

    @Test
    void refusesAFieldDefinitionWithoutAValidProjectAndItemType() throws Exception {
        createField("""
                {"fieldKey":"noScope","label":"x","fieldType":"TEXT"}
                """).andExpect(status().isBadRequest());
        createField("""
                {"project":"NOPE","fieldKey":"x","label":"x","fieldType":"TEXT"}
                """).andExpect(status().isNotFound());
        createField(textField("[\"Missing\"]", "x")).andExpect(status().isBadRequest());
    }

    @Test
    void anotherTenantCannotListAProjectsFieldDefinitions() throws Exception {
        mockMvc.perform(get("/api/fields/schemas?project=PROJ").with(otherTenantUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void anotherTenantCannotReadOrAddComments() throws Exception {
        perform(post("/api/items"), "alice", ITEM).andExpect(status().isCreated());
        perform(post("/api/items/PROJ-1/comments"), "alice", "{\"content\":\"Private\"}")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/items/PROJ-1/comments").with(otherTenantUser()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/items/PROJ-1/comments").with(otherTenantUser())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Hello\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anotherTenantCannotReadChangeOrTransitionAnItem() throws Exception {
        perform(post("/api/items"), "alice", ITEM).andExpect(status().isCreated());

        performAs("tenant-other", get("/api/items/PROJ-1"), null).andExpect(status().isNotFound());
        performAs("tenant-other", patch("/api/items/PROJ-1"), "{\"title\":\"Hijacked\"}")
                .andExpect(status().isNotFound());
        performAs("tenant-other", post("/api/items/PROJ-1/transitions"), "{\"transitionId\":\"startWork\"}")
                .andExpect(status().isNotFound());
        performAs("tenant-other", get("/api/items"), null).andExpect(jsonPath("$.totalElements").value(0));

        perform(get("/api/items/PROJ-1"), "alice", null)
                .andExpect(jsonPath("$.title").value("Fix login"))
                .andExpect(jsonPath("$.statusName").value("Open"));
    }

    private void deployUnchecked(String name, String bpmnXml) {
        repositoryService.createDeployment().addString(name + ".bpmn20.xml", bpmnXml).tenantId(tenant).deploy();
    }

    private ResultActions transition(String transitionId) throws Exception {
        return perform(post("/api/items/PROJ-1/transitions"), "alice",
                "{\"transitionId\":\"" + transitionId + "\"}");
    }

    private ResultActions completeStep(String stepId) throws Exception {
        return perform(post("/api/items/PROJ-1/steps"), "alice", "{\"stepId\":\"" + stepId + "\"}");
    }


    private UUID itemId(String key) {
        return asTenant(() -> itemRepository.findByKey(key)).orElseThrow().getId();
    }

    private List<String> auditEvents(String key) {
        return asTenant(() -> auditEntryRepository.findAll()).stream()
                .filter(entry -> key.equals(entry.getEntityId()))
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .map(AuditEntry::getEventType)
                .toList();
    }

    private List<String> notificationTitles(String user) {
        return asTenant(() -> notificationRepository.findAll()).stream()
                .filter(notification -> notification.getUserId().equals(user))
                .map(Notification::getTitle)
                .toList();
    }

    private ResultActions updateProject(String body) throws Exception {
        return perform(put("/api/projects/PROJ"), "admin", body);
    }

    private ResultActions createField(String body) throws Exception {
        return perform(post("/api/fields/schemas"), "admin", body);
    }

    private static String textField(String itemTypes, String key) {
        return """
                {"project":"PROJ","itemTypes":%s,"fieldKey":"%s","label":"%s","fieldType":"TEXT"}
                """.formatted(itemTypes, key, key);
    }

    private RequestPostProcessor otherTenantUser() {
        return jwt().jwt(token -> token.claim("preferred_username", "mallory").claim("tenant_id", "tenant-other"))
                .authorities(TestRoles.USER);
    }

    private <T> T asTenant(Supplier<T> query) {
        Object[] result = new Object[1];
        TenantContext.runAs(tenant, () -> result[0] = query.get());
        @SuppressWarnings("unchecked")
        T typed = (T) result[0];
        return typed;
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String user, String body) throws Exception {
        return send(request, tenant, user, body);
    }

    private ResultActions performAs(String otherTenant, MockHttpServletRequestBuilder request, String body)
            throws Exception {
        return send(request, otherTenant, "mallory", body);
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String tenantId, String user, String body)
            throws Exception {
        request.with(jwt().jwt(token -> token.claim("preferred_username", user)
                .claim("tenant_id", tenantId)).authorities("admin".equals(user) ? TestRoles.ADMIN : TestRoles.USER));
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }
}
