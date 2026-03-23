Feature: Exceptions are handled and a custom message is sent to the client
  If any exception occurs, it should be handled and a custom message
  with the right HTTP code should be sent back to the client.

  Scenario: Caller with an object that will result in a BadRequest exception
    Given the caller has an object that will result in a BadRequest exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 400
    And the response has status 400 and title "Bad Request"

  Scenario: Caller with an object that will result in a NotFound exception
    Given the caller has an object that will result in a NotFound exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 404
    And the response has status 404 and title "Not Found"

  Scenario: Caller with an object that will result in a Conflict exception
    Given the caller has an object that will result in a Conflict exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 409
    And the response has status 409 and title "Conflict"

  Scenario: Caller with an object that will result in a Unauthorized exception
    Given the caller has an object that will result in a Unauthorized exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 401
    And the response has status 401 and title "Unauthorized"

  Scenario: Caller with an object that will result in a Forbidden exception
    Given the caller has an object that will result in a Forbidden exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 403
    And the response has status 403 and title "Forbidden"

  Scenario: Caller with an object that will result in a RequestTimeout exception
    Given the caller has an object that will result in a RequestTimeout exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 408
    And the response has status 408 and title "Request Timeout"

  Scenario: Caller with an object that will result in a InternalServerError exception
    Given the caller has an object that will result in a InternalServerError exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 500
    And the response has status 500 and title "Internal Server Error"

  Scenario: Caller with an object that will result in a MethodNotAllowed exception
    Given the caller has an object that will result in a MethodNotAllowed exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 405
    And the response has status 405 and title "Method Not Allowed"

  Scenario: Caller with an object that will result in a TooManyRequests exception
    Given the caller has an object that will result in a TooManyRequests exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 429
    And the response has status 429 and title "Too Many Requests"

  Scenario: Caller with an object that will result in a ServiceUnavailable exception
    Given the caller has an object that will result in a ServiceUnavailable exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 503
    And the response has status 503 and title "Service Unavailable"

  Scenario: Caller with an object that will result in a BadGateway exception
    Given the caller has an object that will result in a BadGateway exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 502
    And the response has status 502 and title "Bad Gateway"

  Scenario: Caller with an object that will result in a GatewayTimeout exception
    Given the caller has an object that will result in a GatewayTimeout exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 504
    And the response has status 504 and title "Gateway Timeout"

  Scenario: Caller with an object that will result in a UnprocessableEntity exception
    Given the caller has an object that will result in a UnprocessableEntity exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 422
    And the response has status 422 and title "Unprocessable Content"

  Scenario: Caller with an object that will result in a Gone exception
    Given the caller has an object that will result in a Gone exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 410
    And the response has status 410 and title "Gone"

  Scenario: Caller with an object that will result in a PreconditionFailed exception
    Given the caller has an object that will result in a PreconditionFailed exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 412
    And the response has status 412 and title "Precondition Failed"

  Scenario: Caller with an object that will result in a UnsupportedMediaType exception
    Given the caller has an object that will result in a UnsupportedMediaType exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 415
    And the response has status 415 and title "Unsupported Media Type"

  Scenario: Caller with an object that will result in a unexpected exception
    When the external service calls the "/v1/testFail" endpoint
    Then the service returns HTTP 500
    And the response has status 500 and title "Internal Server Error"

  Scenario: Exception with error code includes code in response
    Given the caller has an exception with code "VALIDATION_001"
    When the external service calls the "/v1/testExceptionWithCode" endpoint
    Then the service returns HTTP 400
    And the response has status 400 and title "Bad Request"
    And the response contains code "VALIDATION_001"

  Scenario: Exception without error code omits code from response
    Given the caller has an object that will result in a BadRequest exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 400
    And the response does not contain code

  Scenario: Source is auto-populated from spring.application.name
    Given the caller has an object that will result in a BadRequest exception
    When the external service calls the "/v1/testException" endpoint
    Then the service returns HTTP 400
    And the response contains source "test-service"

  Scenario: Exception with metadata includes metadata in response
    Given the caller has an exception with metadata
    When the external service calls the "/v1/testExceptionWithMetadata" endpoint
    Then the service returns HTTP 400
    And the response contains metadata field "field" with value "email"
    And the response contains metadata field "limit" with value 255
