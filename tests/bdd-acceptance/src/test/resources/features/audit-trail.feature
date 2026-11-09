Feature: Audit Trail
  As an administrator
  I want every significant action to be recorded in the audit log
  So that I have a complete history of platform activity

  Background:
    Given I am authenticated as "admin-a"
    And the "bddTracker" tracker workflow is deployed
    And project "BDD" uses "bddTracker" for item type "Task"

  Scenario: Creating an item creates an audit entry
    Given I record the current audit entry count
    When I create a "Task" item "Audited" in project "BDD"
    Then the audit log has more entries than before

  Scenario: Moving an item creates an audit entry
    Given I create a "Task" item "Audited move" in project "BDD"
    And I record the current audit entry count
    When I apply the transition "accept"
    Then the audit log has more entries than before

  Scenario: Audit entries are tenant-isolated
    Given I create a "Task" item "Private audit" in project "BDD"
    When I authenticate as "admin-b" and query the audit log
    Then none of the audit entries are about that item
