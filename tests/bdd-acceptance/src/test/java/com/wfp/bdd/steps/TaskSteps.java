package com.wfp.bdd.steps;

import com.wfp.bdd.config.ApiClient;
import com.wfp.bdd.config.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class TaskSteps {

    private final ScenarioContext context;
    private final ApiClient api;

    public TaskSteps(ScenarioContext context, ApiClient api) {
        this.context = context;
        this.api = api;
    }

    // -------------------------------------------------------------------------
    // Then / assertion steps
    // -------------------------------------------------------------------------

    @Then("a task is assigned to {string} for that process instance")
    public void taskAssignedToUserForProcess(String assignee) {
        Response response = api.listTasksForAssignee(assignee);
        assertThat(response.statusCode()).isEqualTo(200);

        List<Map<String, Object>> tasks = response.jsonPath().getList("content");
        Optional<Map<String, Object>> matchingTask = tasks.stream()
                .filter(t -> context.getLastProcessInstanceId().equals(t.get("processInstanceId")))
                .findFirst();

        assertThat(matchingTask)
                .as("Expected a task assigned to '%s' for process instance '%s'",
                        assignee, context.getLastProcessInstanceId())
                .isPresent();
        context.setLastTaskId((String) matchingTask.get().get("id"));
    }

    @Then("no active tasks remain for that process instance")
    public void noActiveTasksRemainForProcess() {
        String assignee = context.getCurrentUsername();
        Response response = api.listTasksForAssignee(assignee);
        List<Map<String, Object>> tasks = response.jsonPath().getList("content");

        boolean hasTaskForProcess = tasks.stream()
                .anyMatch(t -> context.getLastProcessInstanceId().equals(t.get("processInstanceId")));

        assertThat(hasTaskForProcess)
                .as("No tasks should remain for completed process instance '%s'",
                        context.getLastProcessInstanceId())
                .isFalse();
    }

    @Then("the task is now assigned to {string}")
    public void taskIsNowAssignedTo(String expectedAssignee) {
        Response taskResponse = api.getTask(context.getLastTaskId());
        assertThat(taskResponse.statusCode()).isEqualTo(200);
        String actualAssignee = taskResponse.jsonPath().getString("assignee");
        assertThat(actualAssignee)
                .as("Task assignee after delegation")
                .isEqualTo(expectedAssignee);
    }

    // -------------------------------------------------------------------------
    // When / action steps
    // -------------------------------------------------------------------------

    @When("I complete the task assigned to me")
    public void iCompleteTaskAssignedToMe() {
        String assignee = context.getCurrentUsername();
        Map<String, Object> myTask = findTaskForCurrentProcess(assignee);

        String taskId = (String) myTask.get("id");
        context.setLastTaskId(taskId);

        Response completeResponse = api.completeTask(taskId);
        assertThat(completeResponse.statusCode())
                .as("Complete task should return 204 No Content")
                .isEqualTo(204);
    }

    @When("I delegate my task to {string}")
    public void iDelegateMyTaskTo(String toUser) {
        String assignee = context.getCurrentUsername();
        Map<String, Object> myTask = findTaskForCurrentProcess(assignee);

        String taskId = (String) myTask.get("id");
        context.setLastTaskId(taskId);

        Response delegateResponse = api.delegateTask(taskId, toUser);
        assertThat(delegateResponse.statusCode())
                .as("Delegate task should return 204 No Content")
                .isEqualTo(204);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private Map<String, Object> findTaskForCurrentProcess(String assignee) {
        Response tasksResponse = api.listTasksForAssignee(assignee);
        assertThat(tasksResponse.statusCode()).isEqualTo(200);

        List<Map<String, Object>> tasks = tasksResponse.jsonPath().getList("content");
        return tasks.stream()
                .filter(t -> context.getLastProcessInstanceId().equals(t.get("processInstanceId")))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No task found for assignee '" + assignee
                        + "' and process instance '" + context.getLastProcessInstanceId() + "'"));
    }
}
