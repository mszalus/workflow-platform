Feature: Multi-Tenant Data Isolation
  As the platform
  I must ensure tenants cannot access each other's data
  So that each tenant's information remains confidential

  Scenario: Tenant A cannot see Tenant B's process instances
    Given I am authenticated as "admin-a"
    And the "bddIsolationTest" process definition is deployed
    And I have started a "bddIsolationTest" process instance
    When I authenticate as "admin-b" and list active processes
    Then the process instance from tenant A is not in the list

  Scenario: Tenant B cannot see Tenant A's tasks
    Given I am authenticated as "admin-a"
    And the "bddIsolationTest" process definition is deployed
    And I have started a "bddIsolationTest" process instance
    When I authenticate as "admin-b" and list tasks for "admin-a"
    Then no tasks are returned for that query

  Scenario: Each tenant sees only their own deployments
    Given I am authenticated as "admin-a"
    And the "bddIsolationTest" process definition is deployed
    When I authenticate as "admin-b" and list process definitions
    Then "bddIsolationTest" is not in the definitions list for tenant B
