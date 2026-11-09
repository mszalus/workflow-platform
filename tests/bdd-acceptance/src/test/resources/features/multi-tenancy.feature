Feature: Multi-Tenant Data Isolation
  As the platform
  I must ensure tenants cannot access each other's data
  So that each tenant's information remains confidential

  Background:
    Given I am authenticated as "admin-a"
    And the "bddTracker" tracker workflow is deployed
    And project "BDD" uses "bddTracker" for item type "Task"

  Scenario: Tenant B cannot list Tenant A's items
    Given I create a "Task" item "Private list" in project "BDD"
    When I authenticate as "admin-b" and list items
    Then the item from tenant A is not in the list

  Scenario: Tenant B cannot open Tenant A's item
    Given I create a "Task" item "Private item" in project "BDD"
    When I authenticate as "admin-b" and open that item
    Then I receive HTTP status 404

  Scenario: Each tenant sees only their own deployments
    When I authenticate as "admin-b" and list process definitions
    Then "bddTracker" is not in the definitions list for tenant B
