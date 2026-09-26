package com.wfp.bdd.steps;

import com.wfp.bdd.config.ApiClient;
import com.wfp.bdd.config.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.response.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class AuditSteps {

    private final ScenarioContext context;
    private final ApiClient api;

    private int auditCountBefore;

    public AuditSteps(ScenarioContext context, ApiClient api) {
        this.context = context;
        this.api = api;
    }

    @Given("I record the current audit entry count")
    public void recordCurrentAuditCount() {
        Response response = api.listAuditEntries();
        assertThat(response.statusCode()).isEqualTo(200);
        Integer total = response.jsonPath().getInt("totalElements");
        auditCountBefore = total != null ? total : 0;
    }

    @Then("the audit log has more entries than before")
    public void auditLogHasMoreEntries() {
        Response response = api.listAuditEntries();
        assertThat(response.statusCode()).isEqualTo(200);
        int totalAfter = response.jsonPath().getInt("totalElements");
        assertThat(totalAfter)
                .as("Audit entry count should have increased (was %d)", auditCountBefore)
                .isGreaterThan(auditCountBefore);
    }
}
