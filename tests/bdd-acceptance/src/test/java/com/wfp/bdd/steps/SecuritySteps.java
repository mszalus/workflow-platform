package com.wfp.bdd.steps;

import com.wfp.bdd.config.ScenarioContext;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;

public class SecuritySteps {

    private static final String API = "http://localhost:8081";

    private final ScenarioContext context;

    public SecuritySteps(ScenarioContext context) {
        this.context = context;
    }

    @When("I call the process definitions endpoint without a token")
    public void callWithoutToken() {
        Response response = RestAssured.given()
                .when()
                .get(API + "/api/workflow/deployments");
        context.setLastStatusCode(response.statusCode());
    }

    @When("I call the process definitions endpoint with an invalid token")
    public void callWithInvalidToken() {
        Response response = RestAssured.given()
                .header("Authorization", "Bearer this.is.not.a.valid.jwt.token")
                .when()
                .get(API + "/api/workflow/deployments");
        context.setLastStatusCode(response.statusCode());
    }
}
