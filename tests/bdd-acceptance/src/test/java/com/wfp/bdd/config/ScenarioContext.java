package com.wfp.bdd.config;

/**
 * Per-scenario mutable state shared across all step definition classes.
 * PicoContainer creates one instance per scenario and injects the same instance
 * into every step class that declares it as a constructor parameter.
 */
public class ScenarioContext {

    private String currentToken;
    private String currentUsername;
    private String lastItemKey;
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

    public String getLastItemKey() {
        return lastItemKey;
    }

    public void setLastItemKey(String lastItemKey) {
        this.lastItemKey = lastItemKey;
    }

    public int getLastStatusCode() {
        return lastStatusCode;
    }

    public void setLastStatusCode(int lastStatusCode) {
        this.lastStatusCode = lastStatusCode;
    }
}
