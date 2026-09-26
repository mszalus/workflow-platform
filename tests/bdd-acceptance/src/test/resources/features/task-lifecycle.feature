Feature: Task Lifecycle
  As a user
  I want to manage tasks in my inbox
  So that I can complete my assigned work items

  Scenario: A new process instance creates a task assigned to the initiator
    Given I am authenticated as "admin-a"
    And the "bddTaskLifecycle" process definition is deployed
    When I start a "bddTaskLifecycle" process instance
    Then a task is assigned to "admin-a" for that process instance

  Scenario: Completing a task advances the process
    Given I am authenticated as "admin-a"
    And the "bddTaskLifecycle" process definition is deployed
    And I have started a "bddTaskLifecycle" process instance
    When I complete the task assigned to me
    Then no active tasks remain for that process instance

  Scenario: Delegating a task changes the assignee
    Given I am authenticated as "admin-a"
    And the "bddTaskLifecycle" process definition is deployed
    And I have started a "bddTaskLifecycle" process instance
    When I delegate my task to "user-a"
    Then the task is now assigned to "user-a"
