package com.wfp.bdd.steps;

import com.wfp.bdd.config.ApiClient;
import com.wfp.bdd.config.ScenarioContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
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

    // -------------------------------------------------------------------------
    // Given / setup steps
    // -------------------------------------------------------------------------

    @Given("the {string} process definition is deployed")
    public void ensureProcessDeployed(String processKey) {
        Response listResponse = api.listProcessDefinitions();
        List<String> existingKeys = listResponse.jsonPath().getList("key");
        if (!existingKeys.contains(processKey)) {
            Response deployResponse = api.deployProcess(processKey, processKey);
            assertThat(deployResponse.statusCode())
                    .as("Deploy of %s should succeed", processKey)
                    .isEqualTo(201);
        }
    }

    @Given("I have started a {string} process instance")
    public void iHaveStartedProcess(String processKey) {
        Response response = api.startProcess(processKey);
        assertThat(response.statusCode())
                .as("Starting process %s should succeed", processKey)
                .isEqualTo(201);
        context.setLastProcessInstanceId(response.jsonPath().getString("id"));
    }

    // -------------------------------------------------------------------------
    // When / action steps
    // -------------------------------------------------------------------------

    @When("I deploy the {string} process definition")
    public void iDeployProcess(String processKey) {
        Response response = api.deployProcess(processKey, processKey);
        context.setLastStatusCode(response.statusCode());
    }

    @When("I start a {string} process instance")
    public void iStartProcess(String processKey) {
        Response response = api.startProcess(processKey);
        context.setLastStatusCode(response.statusCode());
        if (response.statusCode() == 201) {
            context.setLastProcessInstanceId(response.jsonPath().getString("id"));
        }
    }

    @When("I cancel the process instance")
    public void iCancelProcessInstance() {
        Response response = api.cancelProcess(context.getLastProcessInstanceId());
        assertThat(response.statusCode())
                .as("Cancel process should return 204 No Content")
                .isEqualTo(204);
        context.setLastStatusCode(response.statusCode());
    }

    @When("I try to start a process with definition key {string}")
    public void iTryToStartProcessWithKey(String processKey) {
        Response response = api.startProcess(processKey);
        context.setLastStatusCode(response.statusCode());
    }

    @When("I deploy invalid BPMN content")
    public void iDeployInvalidBpmn() {
        Response response = api.deployInvalidBpmn();
        context.setLastStatusCode(response.statusCode());
    }

    // -------------------------------------------------------------------------
    // Then / assertion steps
    // -------------------------------------------------------------------------

    @Then("the deployment succeeds with status {int}")
    public void deploymentSucceedsWithStatus(int expectedStatus) {
        assertThat(context.getLastStatusCode())
                .as("Deployment response status")
                .isEqualTo(expectedStatus);
    }

    @Then("{string} appears in the process definitions list")
    public void processAppearsInDefinitionsList(String processKey) {
        Response response = api.listProcessDefinitions();
        List<String> keys = response.jsonPath().getList("key");
        assertThat(keys)
                .as("Process definitions list should contain key '%s'", processKey)
                .contains(processKey);
    }

    @Then("the process instance is created with status {int}")
    public void processInstanceCreatedWithStatus(int expectedStatus) {
        assertThat(context.getLastStatusCode())
                .as("Start process response status")
                .isEqualTo(expectedStatus);
        assertThat(context.getLastProcessInstanceId())
                .as("Process instance ID should be set after creation")
                .isNotNull();
    }

    @Then("the instance appears in my active processes")
    public void instanceAppearsInActiveProcesses() {
        String processInstanceId = context.getLastProcessInstanceId();
        Response response = api.listProcesses();
        List<String> ids = response.jsonPath().getList("content.id");
        assertThat(ids)
                .as("Active processes should contain instance %s", processInstanceId)
                .contains(processInstanceId);
    }

    @Then("the process instance is no longer active")
    public void processInstanceNoLongerActive() {
        String processInstanceId = context.getLastProcessInstanceId();
        Response response = api.listProcesses();
        List<String> ids = response.jsonPath().getList("content.id");
        assertThat(ids)
                .as("Active processes should not contain cancelled instance %s", processInstanceId)
                .doesNotContain(processInstanceId);
    }

    @Then("I receive HTTP status {int}")
    public void iReceiveHttpStatus(int expectedStatus) {
        assertThat(context.getLastStatusCode())
                .as("Expected HTTP status")
                .isEqualTo(expectedStatus);
    }
}
