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

    private List<String> tenantBItemKeys;
    private List<String> tenantBDefinitionKeys;
    private List<String> tenantBAuditEntityIds;

    public TenantIsolationSteps(ScenarioContext context, ApiClient api) {
        this.context = context;
        this.api = api;
    }

    @When("I authenticate as {string} and list items")
    public void authenticateAsTenantBAndListItems(String username) {
        tenantBItemKeys = listAs(username, api::listItems, "content.key");
    }

    @Then("the item from tenant A is not in the list")
    public void itemNotVisibleToTenantB() {
        assertThat(tenantBItemKeys).doesNotContain(context.getLastItemKey());
    }

    @When("I authenticate as {string} and open that item")
    public void authenticateAsTenantBAndOpenItem(String username) {
        String originalToken = context.getCurrentToken();
        context.setCurrentToken(api.obtainToken(username));
        try {
            context.setLastStatusCode(api.getItem(context.getLastItemKey()).statusCode());
        } finally {
            context.setCurrentToken(originalToken);
        }
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

    @Then("none of the audit entries are about that item")
    public void noAuditEntriesAboutTheItem() {
        assertThat(tenantBAuditEntityIds).doesNotContain(context.getLastItemKey());
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
