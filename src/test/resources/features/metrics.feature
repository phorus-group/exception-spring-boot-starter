Feature: Exception metrics are recorded when exceptions are handled
  When metrics are enabled and a MeterRegistry is on the classpath,
  handled exceptions should be recorded as counters with type and status tags.

  Scenario: BaseException metrics are recorded with correct tags
    Given the caller has an object that will result in a BadRequest exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 400
    And the metric "http.server.exceptions" is recorded with type "BadRequest" and status 400

  Scenario: Webflux filter exception metrics are recorded
    Given the caller has a normal object
    When the external service calls the "/v1/failFilter" endpoint
    Then the service returns HTTP 401
    And the metric "http.server.exceptions" is recorded with type "Unauthorized" and status 401

  Scenario: Unhandled exception metrics are recorded as 500
    When the external service calls the "/v1/testFail" endpoint
    Then the service returns HTTP 500
    And the metric "http.server.exceptions" is recorded with type "RuntimeException" and status 500

  Scenario: Multiple exceptions produce separate metric counters
    Given the caller has an object that will result in a NotFound exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 404
    And the metric "http.server.exceptions" is recorded with type "NotFound" and status 404
    Given the caller has an object that will result in a Conflict exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 409
    And the metric "http.server.exceptions" is recorded with type "Conflict" and status 409
