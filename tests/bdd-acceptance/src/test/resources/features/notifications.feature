Feature: Notifications
  As a user
  I receive notifications when work items change

  Background:
    Given I am authenticated as "admin-a"
    And the "bddTracker" tracker workflow is deployed
    And project "BDD" uses "bddTracker" for item type "Task"

  Scenario: Assigning an item notifies the assignee
    When I create a "Task" item "For user-a" in project "BDD" assigned to "user-a"
    And I am authenticated as "user-a"
    Then eventually I receive at least 1 notification

  Scenario: Marking all notifications as read resets the unread count
    Given I create a "Task" item "To be read" in project "BDD" assigned to "user-a"
    And I am authenticated as "user-a"
    And eventually I receive at least 1 notification
    When I mark all my notifications as read
    Then my unread notification count is 0
