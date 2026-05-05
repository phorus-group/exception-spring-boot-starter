Feature: Validation errors are handled and a custom message is sent to the client
  If any validation error occurs, it should be handled and a custom message
  with the right HTTP code should be sent back to the client. The message should be a
  json containing the failing object, the field path, the rejected value, a descriptive
  message, the reserved code derived from the failing Jakarta constraint, and the
  constraint's public attributes as metadata.

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

  Scenario: Validation failure carries top-level VALIDATION_FAILED code
    Given the caller has a blank field that cannot be blank
    When the external service calls the "/v1/test" endpoint
    Then the service returns HTTP 400
    And the response contains code "VALIDATION_FAILED"

  Scenario: NotBlank violation produces BLANK and no metadata
    Given the caller has a payload "{ \"value\": \"\" }"
    When the external service calls the "/v1/testNotBlank" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "BLANK"
    And the validation error at index 0 has no metadata

  Scenario: NotNull violation produces REQUIRED and no metadata
    Given the caller has a payload "{ \"value\": null }"
    When the external service calls the "/v1/testNotNull" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "REQUIRED"
    And the validation error at index 0 has no metadata

  Scenario: NotEmpty on string violation produces REQUIRED and no metadata
    Given the caller has a payload "{ \"value\": \"\" }"
    When the external service calls the "/v1/testNotEmptyString" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "REQUIRED"
    And the validation error at index 0 has no metadata

  Scenario: Null violation produces MUST_BE_NULL and no metadata
    Given the caller has a payload "{ \"value\": \"oops\" }"
    When the external service calls the "/v1/testNull" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "MUST_BE_NULL"
    And the validation error at index 0 has no metadata

  Scenario: Size below min produces TOO_SHORT with metadata and no Jakarta-internal keys
    Given the caller has a payload "{ \"value\": \"x\" }"
    When the external service calls the "/v1/testSize" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "TOO_SHORT"
    And the validation error at index 0 has metadata "min" with int value 2
    And the validation error at index 0 has metadata "max" with int value 10
    And the validation error at index 0 has no metadata key "groups"
    And the validation error at index 0 has no metadata key "payload"
    And the validation error at index 0 has no metadata key "message"

  Scenario: Size above max produces TOO_LONG with metadata
    Given the caller has a payload "{ \"value\": \"abcdefghijk\" }"
    When the external service calls the "/v1/testSize" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "TOO_LONG"
    And the validation error at index 0 has metadata "min" with int value 2
    And the validation error at index 0 has metadata "max" with int value 10

  Scenario: Length below min produces TOO_SHORT with metadata
    Given the caller has a payload "{ \"value\": \"x\" }"
    When the external service calls the "/v1/testLength" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "TOO_SHORT"
    And the validation error at index 0 has metadata "min" with int value 2
    And the validation error at index 0 has metadata "max" with int value 10

  Scenario: Length above max produces TOO_LONG with metadata
    Given the caller has a payload "{ \"value\": \"abcdefghijk\" }"
    When the external service calls the "/v1/testLength" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "TOO_LONG"
    And the validation error at index 0 has metadata "min" with int value 2
    And the validation error at index 0 has metadata "max" with int value 10

  Scenario: Min violation produces TOO_SMALL with metadata
    Given the caller has a payload "{ \"value\": 1 }"
    When the external service calls the "/v1/testMin" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "TOO_SMALL"
    And the validation error at index 0 has metadata "value" with int value 5

  Scenario: DecimalMin violation produces TOO_SMALL with metadata
    Given the caller has a payload "{ \"value\": 1.0 }"
    When the external service calls the "/v1/testDecimalMin" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "TOO_SMALL"
    And the validation error at index 0 has metadata "value" with string value "5.0"
    And the validation error at index 0 has metadata "inclusive" with boolean value "true"

  Scenario: Max violation produces TOO_LARGE with metadata
    Given the caller has a payload "{ \"value\": 99 }"
    When the external service calls the "/v1/testMax" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "TOO_LARGE"
    And the validation error at index 0 has metadata "value" with int value 10

  Scenario: DecimalMax violation produces TOO_LARGE with metadata
    Given the caller has a payload "{ \"value\": 99.0 }"
    When the external service calls the "/v1/testDecimalMax" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "TOO_LARGE"
    And the validation error at index 0 has metadata "value" with string value "10.0"
    And the validation error at index 0 has metadata "inclusive" with boolean value "true"

  Scenario: Range below min produces TOO_SMALL with metadata
    Given the caller has a payload "{ \"value\": 1 }"
    When the external service calls the "/v1/testRange" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "TOO_SMALL"
    And the validation error at index 0 has metadata "min" with int value 5
    And the validation error at index 0 has metadata "max" with int value 10

  Scenario: Range above max produces TOO_LARGE with metadata
    Given the caller has a payload "{ \"value\": 99 }"
    When the external service calls the "/v1/testRange" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "TOO_LARGE"
    And the validation error at index 0 has metadata "min" with int value 5
    And the validation error at index 0 has metadata "max" with int value 10

  Scenario: Positive violation produces MUST_BE_POSITIVE and no metadata
    Given the caller has a payload "{ \"value\": 0 }"
    When the external service calls the "/v1/testPositive" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "MUST_BE_POSITIVE"
    And the validation error at index 0 has no metadata

  Scenario: PositiveOrZero violation produces MUST_BE_POSITIVE_OR_ZERO and no metadata
    Given the caller has a payload "{ \"value\": -1 }"
    When the external service calls the "/v1/testPositiveOrZero" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "MUST_BE_POSITIVE_OR_ZERO"
    And the validation error at index 0 has no metadata

  Scenario: Negative violation produces MUST_BE_NEGATIVE and no metadata
    Given the caller has a payload "{ \"value\": 0 }"
    When the external service calls the "/v1/testNegative" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "MUST_BE_NEGATIVE"
    And the validation error at index 0 has no metadata

  Scenario: NegativeOrZero violation produces MUST_BE_NEGATIVE_OR_ZERO and no metadata
    Given the caller has a payload "{ \"value\": 1 }"
    When the external service calls the "/v1/testNegativeOrZero" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "MUST_BE_NEGATIVE_OR_ZERO"
    And the validation error at index 0 has no metadata

  Scenario: Digits violation produces INVALID_NUMBER_FORMAT with metadata
    Given the caller has a payload "{ \"value\": 1234.567 }"
    When the external service calls the "/v1/testDigits" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "INVALID_NUMBER_FORMAT"
    And the validation error at index 0 has metadata "integer" with int value 3
    And the validation error at index 0 has metadata "fraction" with int value 2

  Scenario: Pattern violation produces INVALID_FORMAT with regexp and flags metadata
    Given the caller has a payload "{ \"value\": \"abc\" }"
    When the external service calls the "/v1/testPattern" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "INVALID_FORMAT"
    And the validation error at index 0 has metadata "regexp" with string value "[A-Z]+"
    And the validation error at index 0 has metadata "flags" as empty array

  Scenario: Email violation produces INVALID_EMAIL with regexp and flags metadata
    Given the caller has a payload "{ \"value\": \"not-an-email\" }"
    When the external service calls the "/v1/testEmail" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "INVALID_EMAIL"
    And the validation error at index 0 has metadata "regexp" with string value ".*"
    And the validation error at index 0 has metadata "flags" as empty array

  Scenario: Past violation produces MUST_BE_PAST and no metadata
    Given the caller has a payload "{ \"value\": \"2999-01-01\" }"
    When the external service calls the "/v1/testPast" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "MUST_BE_PAST"
    And the validation error at index 0 has no metadata

  Scenario: PastOrPresent violation produces MUST_BE_PAST_OR_PRESENT and no metadata
    Given the caller has a payload "{ \"value\": \"2999-01-01\" }"
    When the external service calls the "/v1/testPastOrPresent" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "MUST_BE_PAST_OR_PRESENT"
    And the validation error at index 0 has no metadata

  Scenario: Future violation produces MUST_BE_FUTURE and no metadata
    Given the caller has a payload "{ \"value\": \"1900-01-01\" }"
    When the external service calls the "/v1/testFuture" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "MUST_BE_FUTURE"
    And the validation error at index 0 has no metadata

  Scenario: FutureOrPresent violation produces MUST_BE_FUTURE_OR_PRESENT and no metadata
    Given the caller has a payload "{ \"value\": \"1900-01-01\" }"
    When the external service calls the "/v1/testFutureOrPresent" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "MUST_BE_FUTURE_OR_PRESENT"
    And the validation error at index 0 has no metadata

  Scenario: AssertTrue violation produces MUST_BE_TRUE and no metadata
    Given the caller has a payload "{ \"value\": false }"
    When the external service calls the "/v1/testAssertTrue" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "MUST_BE_TRUE"
    And the validation error at index 0 has no metadata

  Scenario: AssertFalse violation produces MUST_BE_FALSE and no metadata
    Given the caller has a payload "{ \"value\": true }"
    When the external service calls the "/v1/testAssertFalse" endpoint
    Then the service returns HTTP 400
    And the validation error at index 0 contains code "MUST_BE_FALSE"
    And the validation error at index 0 has no metadata
