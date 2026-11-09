package com.wfp.bdd.steps;

import com.wfp.bdd.config.ApiClient;
import com.wfp.bdd.config.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ProcessSteps {

    private final ScenarioContext context;
    private final ApiClient api;

    public ProcessSteps(ScenarioContext context, ApiClient api) {
        this.context = context;
        this.api = api;
    }

    @When("I deploy the {string} process definition")
    public void iDeployProcess(String processKey) {
        Response response = api.deployProcess(processKey, processKey);
        context.setLastStatusCode(response.statusCode());
    }

    @When("I deploy invalid BPMN content")
    public void iDeployInvalidBpmn() {
        Response response = api.deployInvalidBpmn();
        context.setLastStatusCode(response.statusCode());
    }

    @Then("the deployment succeeds with status {int}")
    public void deploymentSucceedsWithStatus(int expectedStatus) {
        assertThat(context.getLastStatusCode())
                .as("Deployment response status")
                .isEqualTo(expectedStatus);
    }

    @Then("{string} appears in the process definitions list")
    public void processAppearsInDefinitionsList(String processKey) {
        Response response = api.listProcessDefinitions();
        assertThat(response.statusCode()).isEqualTo(200);
        List<String> keys = response.jsonPath().getList("key");
        assertThat(keys)
                .as("Process definitions list should contain key '%s'", processKey)
                .contains(processKey);
    }

    @Then("I receive HTTP status {int}")
    public void iReceiveHttpStatus(int expectedStatus) {
        assertThat(context.getLastStatusCode())
                .as("Expected HTTP status")
                .isEqualTo(expectedStatus);
    }
}
