Feature: Custom Fields
  As an admin
  I can define custom fields for a project and users can set them on items

  Background:
    Given I am authenticated as "admin-a"
    And the "bddTracker" tracker workflow is deployed
    And project "BDD" uses "bddTracker" for item type "Task"

  Scenario: Define a field for a project
    When I define the text field "summaryNote" on project "BDD"
    Then the field "summaryNote" is listed for project "BDD"

  Scenario: Store a field value on an item
    Given the text field "notes" is defined on project "BDD"
    When I create a "Task" item "With notes" in project "BDD" with "notes" set to "hello BDD"
    Then the item has "notes" set to "hello BDD"
