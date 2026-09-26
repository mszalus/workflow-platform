Feature: Audit Trail
  As an administrator
  I want every significant action to be recorded in the audit log
  So that I have a complete history of platform activity

  Scenario: Starting a process creates an audit entry
    Given I am authenticated as "admin-a"
    And the "bddAuditTest" process definition is deployed
    And I record the current audit entry count
    When I start a "bddAuditTest" process instance
    Then the audit log has more entries than before

  Scenario: Completing a task creates an audit entry
    Given I am authenticated as "admin-a"
    And the "bddAuditTest" process definition is deployed
    And I have started a "bddAuditTest" process instance
    And I record the current audit entry count
    When I complete the task assigned to me
    Then the audit log has more entries than before

  Scenario: Audit entries are tenant-isolated
    Given I am authenticated as "admin-a"
    And the "bddAuditTest" process definition is deployed
    And I have started a "bddAuditTest" process instance
    When I authenticate as "admin-b" and query the audit log
    Then none of the audit entries belong to tenant A's process
