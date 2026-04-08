Feature: Notifications
  As a user
  I receive notifications when workflow events occur

  Scenario: Starting a process instance triggers a task notification
    Given I am authenticated as "admin-a"
    And the "bddNotifyStart" process definition is deployed
    When I start a "bddNotifyStart" process instance
    Then eventually I receive at least 1 notification

  Scenario: Marking all notifications as read resets the unread count
    Given I am authenticated as "admin-a"
    And the "bddNotifyRead" process definition is deployed
    And I have started a "bddNotifyRead" process instance
    And eventually I receive at least 1 notification
    When I mark all my notifications as read
    Then my unread notification count is 0
