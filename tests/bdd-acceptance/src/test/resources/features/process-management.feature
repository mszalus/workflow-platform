Feature: Process Management
  As an admin user
  I want to manage BPMN processes
  So that users can complete work through structured workflows

  Scenario: Deploy a BPMN process definition
    Given I am authenticated as "admin-a"
    When I deploy the "bddProcessMgmt" process definition
    Then the deployment succeeds with status 201
    And "bddProcessMgmt" appears in the process definitions list

  Scenario: Start a process instance
    Given I am authenticated as "admin-a"
    And the "bddProcessMgmt" process definition is deployed
    When I start a "bddProcessMgmt" process instance
    Then the process instance is created with status 201
    And the instance appears in my active processes

  Scenario: Cancel a running process instance
    Given I am authenticated as "admin-a"
    And the "bddProcessMgmt" process definition is deployed
    And I have started a "bddProcessMgmt" process instance
    When I cancel the process instance
    Then the process instance is no longer active

  Scenario: Starting a non-existent process definition is rejected
    Given I am authenticated as "admin-a"
    When I try to start a process with definition key "nonExistentBddProcess99999"
    Then I receive HTTP status 404

  Scenario: Deploying invalid BPMN XML returns an error
    Given I am authenticated as "admin-a"
    When I deploy invalid BPMN content
    Then I receive HTTP status 400
