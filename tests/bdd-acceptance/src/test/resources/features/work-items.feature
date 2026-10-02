Feature: Work items
  As a team member
  I want to move work items through their workflow
  So that everyone can see where each item stands

  Background:
    Given I am authenticated as "admin-a"
    And the "bddTracker" tracker workflow is deployed
    And project "BDD" uses "bddTracker" for item type "Task"

  Scenario: An item moves from Open to Closed
    When I create a "Task" item "Write release notes" in project "BDD"
    Then the item is in status "Open" of category "OPEN"
    When I apply the transition "accept"
    Then the item is in status "Doing" of category "IN_PROGRESS"
    When I apply the transition "finish"
    And I apply the transition "close"
    Then the item is in status "Closed" of category "DONE"
    And the item offers no transitions

  Scenario: A transition the current status does not offer is refused
    When I create a "Task" item "Investigate flaky test" in project "BDD"
    And I try the transition "close"
    Then the transition is refused with status 400
