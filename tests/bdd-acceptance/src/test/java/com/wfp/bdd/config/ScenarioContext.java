package com.wfp.bdd.config;

/**
 * Per-scenario mutable state shared across all step definition classes.
 * PicoContainer creates one instance per scenario and injects the same instance
 * into every step class that declares it as a constructor parameter.
 */
public class ScenarioContext {

    private String currentToken;
    private String currentUsername;
    private String lastProcessInstanceId;
    private String lastTaskId;
    private int lastStatusCode;

    public String getCurrentToken() {
        return currentToken;
    }

    public void setCurrentToken(String currentToken) {
        this.currentToken = currentToken;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void setCurrentUsername(String currentUsername) {
        this.currentUsername = currentUsername;
    }

    public String getLastProcessInstanceId() {
        return lastProcessInstanceId;
    }

    public void setLastProcessInstanceId(String lastProcessInstanceId) {
        this.lastProcessInstanceId = lastProcessInstanceId;
    }

    public String getLastTaskId() {
        return lastTaskId;
    }

    public void setLastTaskId(String lastTaskId) {
        this.lastTaskId = lastTaskId;
    }

    public int getLastStatusCode() {
        return lastStatusCode;
    }

    public void setLastStatusCode(int lastStatusCode) {
        this.lastStatusCode = lastStatusCode;
    }
}
