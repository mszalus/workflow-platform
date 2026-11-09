package com.wfp.bdd.steps;

import com.wfp.bdd.config.ApiClient;
import com.wfp.bdd.config.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkItemSteps {

    private final ScenarioContext context;
    private final ApiClient api;
    private Response lastItem;
    private Response lastTransition;

    public WorkItemSteps(ScenarioContext context, ApiClient api) {
        this.context = context;
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
        createItem(Map.of("project", projectKey, "type", itemType, "title", title));
    }

    @When("I create a {string} item {string} in project {string} assigned to {string}")
    public void createAssignedItem(String itemType, String title, String projectKey, String assignee) {
        createItem(Map.of("project", projectKey, "type", itemType, "title", title, "assignee", assignee));
    }

    @When("I create a {string} item {string} in project {string} with {string} set to {string}")
    public void createItemWithField(String itemType, String title, String projectKey, String field, String value) {
        createItem(Map.of("project", projectKey, "type", itemType, "title", title, "fields", Map.of(field, value)));
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

    @Then("the item has {string} set to {string}")
    public void itemHasField(String field, String value) {
        Response stored = api.getItem(itemKey());
        assertThat(stored.jsonPath().getString("fields." + field)).isEqualTo(value);
    }

    @Then("the item offers no transitions")
    public void itemOffersNoTransitions() {
        assertThat(lastItem.jsonPath().getList("transitions")).isEmpty();
    }

    @Then("the transition is refused with status {int}")
    public void transitionIsRefused(int status) {
        assertThat(lastTransition.statusCode()).isEqualTo(status);
    }

    private void createItem(Map<String, Object> item) {
        lastItem = api.createItem(item);
        assertThat(lastItem.statusCode()).as(lastItem.asString()).isEqualTo(201);
        context.setLastItemKey(itemKey());
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
