Feature: API Security
  As the platform
  I must reject unauthenticated and unauthorized requests
  So that data is protected from unauthorized access

  Scenario: Unauthenticated request is rejected
    When I call the process definitions endpoint without a token
    Then I receive HTTP status 401

  Scenario: Request with an invalid token is rejected
    When I call the process definitions endpoint with an invalid token
    Then I receive HTTP status 401
