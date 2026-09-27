package com.wfp.bdd.steps;

import com.wfp.bdd.config.ApiClient;
import com.wfp.bdd.config.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class TenantIsolationSteps {

    private final ScenarioContext context;
    private final ApiClient api;

    private List<String> tenantBProcessIds;
    private List<String> tenantBTaskProcessIds;
    private List<String> tenantBDefinitionKeys;
    private List<String> tenantBAuditEntityIds;

    public TenantIsolationSteps(ScenarioContext context, ApiClient api) {
        this.context = context;
        this.api = api;
    }

    @When("I authenticate as {string} and list active processes")
    public void authenticateAsTenantBAndListProcesses(String username) {
        tenantBProcessIds = listAs(username, api::listProcesses, "content.id");
    }

    @Then("the process instance from tenant A is not in the list")
    public void processInstanceNotVisibleToTenantB() {
        String instanceId = context.getLastProcessInstanceId();
        assertThat(tenantBProcessIds)
                .as("Tenant B's process list should not contain tenant A's instance %s", instanceId)
                .doesNotContain(instanceId);
    }

    @When("I authenticate as {string} and list tasks for {string}")
    public void authenticateAsTenantBAndListTasks(String tenantBUser, String assignee) {
        tenantBTaskProcessIds = listAs(tenantBUser, () -> api.listTasksForAssignee(assignee),
                "content.processInstanceId");
    }

    @Then("no tasks are returned for that query")
    public void noTasksReturnedForTenantBQuery() {
        String instanceId = context.getLastProcessInstanceId();
        assertThat(tenantBTaskProcessIds)
                .as("Tenant B should not see tasks from tenant A's process %s", instanceId)
                .doesNotContain(instanceId);
    }

    @When("I authenticate as {string} and list process definitions")
    public void authenticateAsTenantBAndListDefinitions(String username) {
        tenantBDefinitionKeys = listAs(username, api::listProcessDefinitions, "key");
    }

    @Then("{string} is not in the definitions list for tenant B")
    public void definitionNotVisibleToTenantB(String processKey) {
        assertThat(tenantBDefinitionKeys)
                .as("Tenant B's definitions list should not contain '%s' (deployed by tenant A)", processKey)
                .doesNotContain(processKey);
    }

    @When("I authenticate as {string} and query the audit log")
    public void authenticateAsTenantBAndQueryAudit(String username) {
        tenantBAuditEntityIds = listAs(username, api::listAuditEntries, "content.entityId");
    }

    @Then("none of the audit entries belong to tenant A's process")
    public void noAuditEntriesFromTenantAProcess() {
        String instanceId = context.getLastProcessInstanceId();
        assertThat(tenantBAuditEntityIds)
                .as("Tenant B's audit log should not contain entries for tenant A's process %s", instanceId)
                .doesNotContain(instanceId);
    }

    private List<String> listAs(String username, Supplier<Response> request, String valuePath) {
        String originalToken = context.getCurrentToken();
        context.setCurrentToken(api.obtainToken(username));
        try {
            Response response = request.get();
            assertThat(response.statusCode())
                    .as("%s's request should succeed before its results are checked", username)
                    .isEqualTo(200);
            return response.jsonPath().getList(valuePath);
        } finally {
            context.setCurrentToken(originalToken);
        }
    }
}
