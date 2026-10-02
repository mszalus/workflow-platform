package com.wfp.workflow;

import com.wfp.security.context.TenantContext;
import com.wfp.workflow.engine.Samples;
import com.wfp.workflow.engine.flowable.DeploymentService;
import com.wfp.workflow.entity.AuditEntry;
import com.wfp.workflow.entity.FieldOption;
import com.wfp.workflow.entity.FieldSchema;
import com.wfp.workflow.entity.FieldType;
import com.wfp.workflow.entity.ItemTransition;
import com.wfp.workflow.entity.Notification;
import com.wfp.workflow.repository.AuditEntryRepository;
import com.wfp.workflow.repository.FieldSchemaRepository;
import com.wfp.workflow.repository.ItemRepository;
import com.wfp.workflow.repository.ItemTransitionRepository;
import com.wfp.workflow.repository.NotificationRepository;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ItemControllerTest {

    private static final String ITEM = """
        {"project":"PROJ","type":"Task","title":"Fix login","assignee":"bob","fields":{"severity":"High"}}
        """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeploymentService deploymentService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private TaskService flowableTaskService;

    @Autowired
    private FieldSchemaRepository fieldSchemaRepository;

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
        TenantContext.runAs(tenant, () -> deploymentService.deploy("simple", null, Samples.xml("valid/simple")));
        FieldSchema severity = FieldSchema.builder().tenantId(tenant).processDefinitionKey("simple")
                .fieldKey("severity").label("Severity").fieldType(FieldType.DROPDOWN).required(true).build();
        severity.getOptions().add(FieldOption.builder().fieldSchema(severity).label("High").value("High").build());
        severity.getOptions().add(FieldOption.builder().fieldSchema(severity).label("Low").value("Low").build());
        fieldSchemaRepository.save(severity);

        perform(post("/api/projects"), "admin", """
                {"key":"PROJ","name":"Platform","itemTypes":[{"name":"Task","workflowKey":"simple"}]}
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
        TenantContext.runAs(tenant, () -> deploymentService.deploy("simple", null, brokenVersion));

        perform(post("/api/items"), "alice", ITEM).andExpect(status().isBadRequest());
    }

    @Test
    void theOldTaskAndProcessApiDoNotSeeItemRuns() throws Exception {
        perform(post("/api/items"), "alice", ITEM).andExpect(status().isCreated());
        String taskId = flowableTaskService.createTaskQuery().taskTenantId(tenant).singleResult().getId();
        String runId = flowableTaskService.createTaskQuery().taskTenantId(tenant).singleResult().getProcessInstanceId();

        perform(get("/api/workflow/tasks"), "alice", null).andExpect(jsonPath("$.totalElements").value(0));
        perform(get("/api/workflow/processes"), "alice", null).andExpect(jsonPath("$.totalElements").value(0));
        perform(post("/api/workflow/tasks/{id}/complete", taskId), "alice", "{}").andExpect(status().isNotFound());
        perform(delete("/api/workflow/processes/{id}", runId), "alice", null).andExpect(status().isNotFound());
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
        TenantContext.runAs(tenant, () -> deploymentService.deploy("plain", null, plainBpmn));

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

    private ResultActions transition(String transitionId) throws Exception {
        return perform(post("/api/items/PROJ-1/transitions"), "alice",
                "{\"transitionId\":\"" + transitionId + "\"}");
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
        request.with(jwt().jwt(token -> token.claim("preferred_username", user).claim("tenant_id", tenantId)));
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }
}
