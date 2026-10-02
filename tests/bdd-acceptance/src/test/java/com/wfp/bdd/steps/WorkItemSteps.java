package com.wfp.bdd.steps;

import com.wfp.bdd.config.ApiClient;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkItemSteps {

    private final ApiClient api;
    private Response lastItem;
    private Response lastTransition;

    public WorkItemSteps(ApiClient api) {
        this.api = api;
    }

    @Given("the {string} tracker workflow is deployed")
    public void trackerWorkflowIsDeployed(String workflowKey) {
        if (!api.listProcessDefinitions().jsonPath().getList("key").contains(workflowKey)) {
            assertThat(api.deployBpmn(workflowKey, workflowXml(workflowKey)).statusCode()).isEqualTo(201);
        }
    }

    @Given("project {string} uses {string} for item type {string}")
    public void projectUsesWorkflow(String projectKey, String workflowKey, String itemType) {
        if (!api.listProjects().jsonPath().getList("key").contains(projectKey)) {
            assertThat(api.createProject(projectKey, itemType, workflowKey).statusCode()).isEqualTo(201);
        }
    }

    @When("I create a {string} item {string} in project {string}")
    public void createItem(String itemType, String title, String projectKey) {
        lastItem = api.createItem(projectKey, itemType, title);
        assertThat(lastItem.statusCode()).isEqualTo(201);
    }

    @When("I apply the transition {string}")
    public void applyTransition(String transitionId) {
        lastItem = api.transitionItem(itemKey(), transitionId);
        assertThat(lastItem.statusCode()).as(lastItem.asString()).isEqualTo(200);
    }

    @When("I try the transition {string}")
    public void tryTransition(String transitionId) {
        lastTransition = api.transitionItem(itemKey(), transitionId);
    }

    @Then("the item is in status {string} of category {string}")
    public void itemIsInStatus(String statusName, String category) {
        assertThat(lastItem.jsonPath().getString("statusName")).isEqualTo(statusName);
        assertThat(lastItem.jsonPath().getString("statusCategory")).isEqualTo(category);
    }

    @Then("the item offers no transitions")
    public void itemOffersNoTransitions() {
        assertThat(lastItem.jsonPath().getList("transitions")).isEmpty();
    }

    @Then("the transition is refused with status {int}")
    public void transitionIsRefused(int status) {
        assertThat(lastTransition.statusCode()).isEqualTo(status);
    }

    private String itemKey() {
        return lastItem.jsonPath().getString("key");
    }

    private static String workflowXml(String workflowKey) {
        try (InputStream xml = WorkItemSteps.class.getResourceAsStream("/workflows/" + workflowKey + ".bpmn20.xml")) {
            return new String(xml.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
