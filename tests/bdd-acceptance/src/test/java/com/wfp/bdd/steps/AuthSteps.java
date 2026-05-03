package com.wfp.bdd.steps;

import com.wfp.bdd.config.ApiClient;
import com.wfp.bdd.config.ScenarioContext;
import io.cucumber.java.en.Given;

public class AuthSteps {

    private final ScenarioContext context;
    private final ApiClient api;

    public AuthSteps(ScenarioContext context, ApiClient api) {
        this.context = context;
        this.api = api;
    }

    @Given("I am authenticated as {string}")
    public void iAmAuthenticatedAs(String username) {
        String token = api.obtainToken(username);
        context.setCurrentToken(token);
        context.setCurrentUsername(username);
    }
}
