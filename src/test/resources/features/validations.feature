Feature: Validation errors are handled and a custom message is sent to the client
  If any validation error occurs, it should be handled and a custom message
  with the right HTTP code should be sent back to the client. The message should be a
  json containing:
  - The object that originated the error.
  - The object field that was wrong.
  - The rejected value.
  - A descriptive message about the object field and why the value was rejected.

  Scenario: Caller with a list containing an object with a null field that cannot be null
    Given the caller has a list containing an object with null field that cannot be null
    When the external service calls the "/v1/testList" endpoint
    Then the service returns HTTP 400
    And the service returns a message with the validation errors
      | obj            | field   | rejectedValue | message        |
      | TestController | testInt | null          | Cannot be null |

  Scenario: Caller with a null field that cannot be null
    Given the caller has a null field that cannot be null
    When the external service calls the "/v1/test" endpoint
    Then the service returns HTTP 400
    And the service returns a message with the validation errors
      | obj        | field   | rejectedValue | message        |
      | testObject | testInt | null          | Cannot be null |

  Scenario: Caller with a blank field that cannot be blank
    Given the caller has a blank field that cannot be blank
    When the external service calls the "/v1/test" endpoint
    Then the service returns HTTP 400
    And the service returns a message with the validation errors
      | obj        | field   | rejectedValue | message         |
      | testObject | testVar | blank         | Cannot be blank |

  Scenario: Caller with a null field that cannot be blank
    Given the caller has a null field that cannot be blank
    When the external service calls the "/v1/test" endpoint
    Then the service returns HTTP 400
    And the service returns a message with the validation errors
      | obj        | field   | rejectedValue | message         |
      | testObject | testVar | null          | Cannot be blank |

  Scenario: Caller with an empty list field that cannot be empty
    Given the caller has an empty list field that cannot be empty
    When the external service calls the "/v1/test" endpoint
    Then the service returns HTTP 400
    And the service returns a message with the validation errors
      | obj        | field         | rejectedValue | message         |
      | testObject | subObjectList | []            | Cannot be empty |

  Scenario: Caller with a null list field that cannot be null
    Given the caller has a null list field that cannot be null
    When the external service calls the "/v1/test" endpoint
    Then the service returns HTTP 400
    And the service returns a message with the validation errors
      | obj        | field         | rejectedValue | message         |
      | testObject | subObjectList | null          | Cannot be empty |

  Scenario: Caller with a blank subfield that cannot be blank
    Given the caller has a blank subfield that cannot be blank
    When the external service calls the "/v1/test" endpoint
    Then the service returns HTTP 400
    And the service returns a message with the validation errors
      | obj        | field             | rejectedValue | message         |
      | testObject | subObject.testVar | blank         | Cannot be blank |