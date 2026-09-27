package com.wfp.bdd.steps;

import com.wfp.bdd.config.ApiClient;
import com.wfp.bdd.config.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.response.Response;
import org.awaitility.Awaitility;

import java.util.concurrent.TimeUnit;

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
        auditCountBefore = currentAuditCount();
    }

    @Then("the audit log has more entries than before")
    public void auditLogHasMoreEntries() {
        Awaitility.await("audit entry recorded")
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(currentAuditCount())
                        .as("Audit entry count should have increased (was %d)", auditCountBefore)
                        .isGreaterThan(auditCountBefore));
    }

    private int currentAuditCount() {
        Response response = api.listAuditEntries();
        assertThat(response.statusCode()).isEqualTo(200);
        Integer total = response.jsonPath().getInt("totalElements");
        return total != null ? total : 0;
    }
}
