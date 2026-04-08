package com.wfp.bdd.steps;

import com.wfp.bdd.config.ApiClient;
import com.wfp.bdd.config.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TenantIsolationSteps {

    private final ScenarioContext context;
    private final ApiClient api;

    // Holds the token obtained after switching tenants mid-scenario
    private String tenantBToken;

    public TenantIsolationSteps(ScenarioContext context, ApiClient api) {
        this.context = context;
        this.api = api;
    }

    @When("I authenticate as {string} and list active processes")
    public void authenticateAsTenantBAndListProcesses(String username) {
        tenantBToken = api.obtainToken(username);
        // Temporarily switch context to make the request
        String originalToken = context.getCurrentToken();
        context.setCurrentToken(tenantBToken);
        Response response = api.listProcesses();
        context.setCurrentToken(originalToken);
        context.setLastStatusCode(response.statusCode());
        // Store the IDs returned by tenant B for the assertion
        List<String> ids = response.jsonPath().getList("content.id");
        // We reuse lastResponseBody as a delimited string for the assertion step
        context.setLastStatusCode(response.statusCode());
        // Save IDs via a simple approach: store in a thread-local-like field
        tenantBProcessIds = ids;
    }

    private List<String> tenantBProcessIds;
    private List<String> tenantBTaskIds;
    private List<String> tenantBDefinitionKeys;
    private List<String> tenantBAuditProcessIds;

    @Then("the process instance from tenant A is not in the list")
    public void processInstanceNotVisibleToTenantB() {
        String instanceId = context.getLastProcessInstanceId();
        assertThat(tenantBProcessIds)
                .as("Tenant B's process list should not contain tenant A's instance %s", instanceId)
                .doesNotContain(instanceId);
    }

    @When("I authenticate as {string} and list tasks for {string}")
    public void authenticateAsTenantBAndListTasks(String tenantBUser, String assignee) {
        tenantBToken = api.obtainToken(tenantBUser);
        String originalToken = context.getCurrentToken();
        context.setCurrentToken(tenantBToken);
        Response response = api.listTasksForAssignee(assignee);
        context.setCurrentToken(originalToken);

        List<String> taskProcessIds = response.jsonPath().getList("content.processInstanceId");
        tenantBTaskIds = taskProcessIds;
    }

    @Then("no tasks are returned for that query")
    public void noTasksReturnedForTenantBQuery() {
        String instanceId = context.getLastProcessInstanceId();
        // Either the list is empty, or none of the tasks belong to tenant A's process
        assertThat(tenantBTaskIds)
                .as("Tenant B should not see tasks from tenant A's process %s", instanceId)
                .doesNotContain(instanceId);
    }

    @When("I authenticate as {string} and list process definitions")
    public void authenticateAsTenantBAndListDefinitions(String username) {
        tenantBToken = api.obtainToken(username);
        String originalToken = context.getCurrentToken();
        context.setCurrentToken(tenantBToken);
        Response response = api.listProcessDefinitions();
        context.setCurrentToken(originalToken);
        tenantBDefinitionKeys = response.jsonPath().getList("key");
    }

    @Then("{string} is not in the definitions list for tenant B")
    public void definitionNotVisibleToTenantB(String processKey) {
        assertThat(tenantBDefinitionKeys)
                .as("Tenant B's definitions list should not contain '%s' (deployed by tenant A)", processKey)
                .doesNotContain(processKey);
    }

    @When("I authenticate as {string} and query the audit log")
    public void authenticateAsTenantBAndQueryAudit(String username) {
        tenantBToken = api.obtainToken(username);
        String originalToken = context.getCurrentToken();
        context.setCurrentToken(tenantBToken);
        Response response = api.listAuditEntries();
        context.setCurrentToken(originalToken);

        // Collect all entity IDs from tenant B's audit entries
        List<String> entityIds = response.jsonPath().getList("content.entityId");
        tenantBAuditProcessIds = entityIds;
    }

    @Then("none of the audit entries belong to tenant A's process")
    public void noAuditEntriesFromTenantAProcess() {
        String instanceId = context.getLastProcessInstanceId();
        assertThat(tenantBAuditProcessIds)
                .as("Tenant B's audit log should not contain entries for tenant A's process %s", instanceId)
                .doesNotContain(instanceId);
    }
}
