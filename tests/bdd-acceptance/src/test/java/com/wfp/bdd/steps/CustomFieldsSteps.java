package com.wfp.bdd.steps;

import com.wfp.bdd.config.ApiClient;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomFieldsSteps {

    private final ApiClient api;

    public CustomFieldsSteps(ApiClient api) {
        this.api = api;
    }

    @When("I define the text field {string} on project {string}")
    @Given("the text field {string} is defined on project {string}")
    public void defineField(String fieldKey, String project) {
        if (!fieldKeys(project).contains(fieldKey)) {
            Response response = api.createTextField(project, fieldKey);
            assertThat(response.statusCode()).as(response.asString()).isEqualTo(201);
        }
    }

    @Then("the field {string} is listed for project {string}")
    public void fieldIsListed(String fieldKey, String project) {
        assertThat(fieldKeys(project)).contains(fieldKey);
    }

    private List<String> fieldKeys(String project) {
        Response response = api.listFields(project);
        assertThat(response.statusCode()).isEqualTo(200);
        return response.jsonPath().getList("fieldKey");
    }
}
