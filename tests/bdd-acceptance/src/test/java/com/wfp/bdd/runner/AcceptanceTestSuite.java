package com.wfp.bdd.runner;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * JUnit Platform Suite runner for all BDD acceptance tests.
 *
 * Prerequisites: the full Docker stack must be running on localhost
 * (gateway on port 9080, Keycloak on port 8180).
 *
 * Run:  ./gradlew :tests:bdd-acceptance:test
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(
        key = Constants.GLUE_PROPERTY_NAME,
        value = "com.wfp.bdd")
@ConfigurationParameter(
        key = Constants.PLUGIN_PROPERTY_NAME,
        value = "pretty,"
                + "html:build/reports/cucumber/cucumber-report.html,"
                + "json:build/reports/cucumber/cucumber-report.json")
@ConfigurationParameter(
        key = Constants.EXECUTION_DRY_RUN_PROPERTY_NAME,
        value = "false")
public class AcceptanceTestSuite {
}
