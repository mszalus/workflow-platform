package com.wfp.bdd.config;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.List;
import java.util.Map;

/**
 * HTTP client wrapper for all API calls made by BDD step definitions.
 * Uses the token stored in ScenarioContext for authentication.
 */
public class ApiClient {

    private static final String API = "http://localhost:8081";
    private static final String KEYCLOAK = "http://localhost:8180";
    private static final String TOKEN_URL =
            KEYCLOAK + "/realms/workflow-platform/protocol/openid-connect/token";

    private final ScenarioContext context;

    public ApiClient(ScenarioContext context) {
        this.context = context;
    }

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------

    public String obtainToken(String username) {
        return RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "password")
                .formParam("client_id", "wfp-web")
                .formParam("username", username)
                .formParam("password", "password")
                .when()
                .post(TOKEN_URL)
                .then()
                .statusCode(200)
                .extract().jsonPath().getString("access_token");
    }

    // -------------------------------------------------------------------------
    // Process definitions / deployments
    // -------------------------------------------------------------------------

    public Response deployProcess(String key, String name) {
        return auth()
                .body(Map.of("name", name, "bpmnXml", buildBpmn(key, name)))
                .when()
                .post(API + "/api/workflow/deployments");
    }

    public Response deployInvalidBpmn() {
        return auth()
                .body(Map.of("name", "BDDInvalidBpmn", "bpmnXml", "this-is-not-valid-xml"))
                .when()
                .post(API + "/api/workflow/deployments");
    }

    public Response listProcessDefinitions() {
        return auth().when().get(API + "/api/workflow/deployments");
    }

    // -------------------------------------------------------------------------
    // Custom fields
    // -------------------------------------------------------------------------

    public Response createTextField(String project, String fieldKey) {
        return auth()
                .body(Map.of("project", project, "fieldKey", fieldKey, "label", fieldKey, "fieldType", "TEXT"))
                .when()
                .post(API + "/api/fields/schemas");
    }

    public Response listFields(String project) {
        return auth().when().get(API + "/api/fields/schemas?project=" + project);
    }

    // -------------------------------------------------------------------------
    // Notifications
    // -------------------------------------------------------------------------

    public Response listNotifications() {
        return auth().when().get(API + "/api/notifications?size=100");
    }

    public long getUnreadCount() {
        return auth().when()
                .get(API + "/api/notifications/unread-count")
                .jsonPath().getLong("count");
    }

    public Response markAllNotificationsRead() {
        return auth().when().put(API + "/api/notifications/mark-all-read");
    }

    // -------------------------------------------------------------------------
    // Audit
    // -------------------------------------------------------------------------

    public Response listAuditEntries() {
        return auth().when().get(API + "/api/audit?size=100");
    }

    // -------------------------------------------------------------------------
    // Work items
    // -------------------------------------------------------------------------

    public Response deployBpmn(String name, String bpmnXml) {
        return auth()
                .body(Map.of("name", name, "bpmnXml", bpmnXml))
                .when()
                .post(API + "/api/workflow/deployments");
    }

    public Response listProjects() {
        return auth().when().get(API + "/api/projects");
    }

    public Response createProject(String key, String itemType, String workflowKey) {
        return auth()
                .body(Map.of("key", key, "name", key,
                        "itemTypes", List.of(Map.of("name", itemType, "workflowKey", workflowKey))))
                .when()
                .post(API + "/api/projects");
    }

    public Response createItem(Map<String, Object> item) {
        return auth().body(item).when().post(API + "/api/items");
    }

    public Response listItems() {
        return auth().when().get(API + "/api/items?size=100");
    }

    public Response getItem(String key) {
        return auth().when().get(API + "/api/items/" + key);
    }

    public Response transitionItem(String key, String transitionId) {
        return auth()
                .body(Map.of("transitionId", transitionId))
                .when()
                .post(API + "/api/items/" + key + "/transitions");
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private RequestSpecification auth() {
        return RestAssured.given()
                .header("Authorization", "Bearer " + context.getCurrentToken())
                .contentType("application/json");
    }

    /**
     * Builds a minimal single-user-task BPMN process definition.
     * The task is automatically assigned to the process initiator via a Flowable EL expression.
     */
    private static String buildBpmn(String key, String name) {
        // Note: ${initiator} is a Flowable EL expression — not a Java format placeholder.
        // String.format only replaces %s/%d patterns, so ${initiator} passes through unchanged.
        return String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\""
                + " xmlns:flowable=\"http://flowable.org/bpmn\""
                + " targetNamespace=\"http://wfp.com/bdd\">"
                + "<process id=\"%s\" name=\"%s\" isExecutable=\"true\">"
                + "<startEvent id=\"start\" flowable:initiator=\"initiator\"/>"
                + "<userTask id=\"task1\" name=\"BDD Task\" flowable:assignee=\"${initiator}\"/>"
                + "<endEvent id=\"end\"/>"
                + "<sequenceFlow id=\"sf1\" sourceRef=\"start\" targetRef=\"task1\"/>"
                + "<sequenceFlow id=\"sf2\" sourceRef=\"task1\" targetRef=\"end\"/>"
                + "</process>"
                + "</definitions>",
                key, name);
    }
}
