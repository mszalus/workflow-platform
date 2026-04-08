Feature: Custom Fields
  As an admin
  I can define field schemas for process definitions and save values per instance

  Scenario: Create and retrieve a custom field schema
    Given I am authenticated as "admin-a"
    When I create a TEXT field schema with key "priority" for process definition "bddCfSchema"
    Then I receive HTTP status 201
    And the schema "priority" appears in the list for process definition "bddCfSchema"

  Scenario: Save and retrieve field values for a process instance
    Given I am authenticated as "admin-a"
    And the "bddCfValues" process definition is deployed
    And I have created a TEXT field schema with key "notes" for process definition "bddCfValues"
    And I have started a "bddCfValues" process instance
    When I save field value "notes" = "hello BDD" for the current process instance under definition "bddCfValues"
    Then retrieving field values for the current process instance includes "notes" = "hello BDD"
