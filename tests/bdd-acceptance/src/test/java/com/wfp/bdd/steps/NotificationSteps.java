package com.wfp.bdd.steps;

import com.wfp.bdd.config.ApiClient;
import com.wfp.bdd.config.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.awaitility.Awaitility;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class NotificationSteps {

    private final ScenarioContext context;
    private final ApiClient api;

    public NotificationSteps(ScenarioContext context, ApiClient api) {
        this.context = context;
        this.api = api;
    }

    @Then("eventually I receive at least {int} notification(s)")
    public void eventuallyReceiveNotifications(int minCount) {
        Awaitility.await("notification arrives")
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    Integer total = api.listNotifications().jsonPath().getInt("totalElements");
                    return total != null && total >= minCount;
                });
    }

    @When("I mark all my notifications as read")
    public void markAllNotificationsRead() {
        api.markAllNotificationsRead();
    }

    @Then("my unread notification count is {int}")
    public void unreadCountIs(int expectedCount) {
        long count = api.getUnreadCount();
        assertThat(count)
                .as("Unread notification count should be %d", expectedCount)
                .isEqualTo(expectedCount);
    }
}
