package com.wfp.bdd.config;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

/**
 * HTTP client wrapper for all API calls made by BDD step definitions.
 * Uses the token stored in ScenarioContext for authentication.
 */
public class ApiClient {

    private static final String GATEWAY = "http://localhost:9080";
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
                .formParam("client_id", "wfp-admin-portal")
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
                .post(GATEWAY + "/api/workflow/deployments");
    }

    public Response deployInvalidBpmn() {
        return auth()
                .body(Map.of("name", "BDDInvalidBpmn", "bpmnXml", "this-is-not-valid-xml"))
                .when()
                .post(GATEWAY + "/api/workflow/deployments");
    }

    public Response listProcessDefinitions() {
        return auth().when().get(GATEWAY + "/api/workflow/deployments");
    }

    // -------------------------------------------------------------------------
    // Process instances
    // -------------------------------------------------------------------------

    public Response startProcess(String processDefinitionKey) {
        return auth()
                .body(Map.of("processDefinitionKey", processDefinitionKey))
                .when()
                .post(GATEWAY + "/api/workflow/processes");
    }

    public Response listProcesses() {
        return auth().when().get(GATEWAY + "/api/workflow/processes?size=100");
    }

    public Response cancelProcess(String processInstanceId) {
        return auth().when()
                .delete(GATEWAY + "/api/workflow/processes/" + processInstanceId);
    }

    // -------------------------------------------------------------------------
    // Tasks
    // -------------------------------------------------------------------------

    public Response listTasksForAssignee(String assignee) {
        return auth().when()
                .get(GATEWAY + "/api/workflow/tasks?assignee=" + assignee + "&size=100");
    }

    public Response getTask(String taskId) {
        return auth().when().get(GATEWAY + "/api/workflow/tasks/" + taskId);
    }

    public Response completeTask(String taskId) {
        return auth()
                .body("{}")
                .when()
                .post(GATEWAY + "/api/workflow/tasks/" + taskId + "/complete");
    }

    public Response delegateTask(String taskId, String toUserId) {
        return auth()
                .body(Map.of("delegateToUserId", toUserId, "comment", "Delegated via BDD test"))
                .when()
                .post(GATEWAY + "/api/workflow/tasks/" + taskId + "/delegate");
    }

    // -------------------------------------------------------------------------
    // Custom fields
    // -------------------------------------------------------------------------

    public Response createFieldSchema(String processDefinitionKey, String fieldKey, String fieldType) {
        return auth()
                .body(Map.of(
                        "processDefinitionKey", processDefinitionKey,
                        "fieldKey", fieldKey,
                        "label", fieldKey,
                        "fieldType", fieldType,
                        "required", false,
                        "sortOrder", 0
                ))
                .when()
                .post(GATEWAY + "/api/fields/schemas");
    }

    public Response listFieldSchemas(String processDefinitionKey) {
        return auth().when()
                .get(GATEWAY + "/api/fields/schemas?processDefinitionKey=" + processDefinitionKey);
    }

    public Response saveFieldValues(String processInstanceId, String processDefinitionKey,
                                    Map<String, String> values) {
        return auth()
                .body(Map.of(
                        "processInstanceId", processInstanceId,
                        "processDefinitionKey", processDefinitionKey,
                        "values", values
                ))
                .when()
                .post(GATEWAY + "/api/fields/values");
    }

    public Response getFieldValues(String processInstanceId) {
        return auth().when()
                .get(GATEWAY + "/api/fields/values?processInstanceId=" + processInstanceId);
    }

    // -------------------------------------------------------------------------
    // Notifications
    // -------------------------------------------------------------------------

    public Response listNotifications() {
        return auth().when().get(GATEWAY + "/api/notifications?size=100");
    }

    public long getUnreadCount() {
        return auth().when()
                .get(GATEWAY + "/api/notifications/unread-count")
                .jsonPath().getLong("count");
    }

    public Response markAllNotificationsRead() {
        return auth().when().put(GATEWAY + "/api/notifications/mark-all-read");
    }

    // -------------------------------------------------------------------------
    // Audit
    // -------------------------------------------------------------------------

    public Response listAuditEntries() {
        return auth().when().get(GATEWAY + "/api/audit?size=100");
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
