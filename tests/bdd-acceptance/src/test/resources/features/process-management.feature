Feature: Workflow Deployment
  As an admin user
  I want to deploy BPMN workflows
  So that work items can move through them

  Scenario: Deploy a BPMN process definition
    Given I am authenticated as "admin-a"
    When I deploy the "bddProcessMgmt" process definition
    Then the deployment succeeds with status 201
    And "bddProcessMgmt" appears in the process definitions list

  Scenario: Deploying invalid BPMN XML returns an error
    Given I am authenticated as "admin-a"
    When I deploy invalid BPMN content
    Then I receive HTTP status 400
