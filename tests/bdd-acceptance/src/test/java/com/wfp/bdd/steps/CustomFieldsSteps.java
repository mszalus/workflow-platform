package com.wfp.bdd.steps;

import com.wfp.bdd.config.ApiClient;
import com.wfp.bdd.config.ScenarioContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomFieldsSteps {

    private final ScenarioContext context;
    private final ApiClient api;

    public CustomFieldsSteps(ScenarioContext context, ApiClient api) {
        this.context = context;
        this.api = api;
    }

    @When("I create a TEXT field schema with key {string} for process definition {string}")
    public void createFieldSchema(String fieldKey, String processDefinitionKey) {
        Response response = api.createFieldSchema(processDefinitionKey, fieldKey, "TEXT");
        context.setLastStatusCode(response.statusCode());
    }

    @And("I have created a TEXT field schema with key {string} for process definition {string}")
    public void iHaveCreatedFieldSchema(String fieldKey, String processDefinitionKey) {
        Response response = api.createFieldSchema(processDefinitionKey, fieldKey, "TEXT");
        assertThat(response.statusCode())
                .as("Creating field schema '%s' should succeed", fieldKey)
                .isEqualTo(201);
    }

    @And("the schema {string} appears in the list for process definition {string}")
    public void schemaAppearsInList(String fieldKey, String processDefinitionKey) {
        Response response = api.listFieldSchemas(processDefinitionKey);
        assertThat(response.statusCode()).isEqualTo(200);
        List<String> keys = response.jsonPath().getList("fieldKey");
        assertThat(keys)
                .as("Field schemas for '%s' should contain key '%s'", processDefinitionKey, fieldKey)
                .contains(fieldKey);
    }

    @When("I save field value {string} = {string} for the current process instance under definition {string}")
    public void saveFieldValue(String fieldKey, String value, String processDefinitionKey) {
        String processInstanceId = context.getLastProcessInstanceId();
        Response response = api.saveFieldValues(processInstanceId, processDefinitionKey,
                Map.of(fieldKey, value));
        context.setLastStatusCode(response.statusCode());
        assertThat(response.statusCode())
                .as("Saving field value should return 201")
                .isEqualTo(201);
    }

    @Then("retrieving field values for the current process instance includes {string} = {string}")
    public void fieldValueIsRetrieved(String fieldKey, String expectedValue) {
        String processInstanceId = context.getLastProcessInstanceId();
        Response response = api.getFieldValues(processInstanceId);
        assertThat(response.statusCode()).isEqualTo(200);
        List<Map<String, String>> values = response.jsonPath().getList("");
        boolean found = values.stream()
                .anyMatch(v -> fieldKey.equals(v.get("fieldKey")) && expectedValue.equals(v.get("value")));
        assertThat(found)
                .as("Field values should contain '%s' = '%s'", fieldKey, expectedValue)
                .isTrue();
    }
}
