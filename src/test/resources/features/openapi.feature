Feature: OpenAPI integration emits the x-validations extension for fields carrying Jakarta constraints
  When springdoc is on the classpath, properties annotated with Jakarta validation
  constraints expose an x-validations array on their schema. Each entry has shape
  { rule, code } where rule is the JSON Schema vocabulary keyword (or a synthetic
  fallback) and code is the reserved error code emitted at runtime when that constraint
  is violated.

  Scenario: Property with multiple constraints emits one entry per constraint
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "KitchenSinkDto" has property "name" with x-validations
      | rule      | code           |
      | notBlank  | BLANK          |
      | minLength | TOO_SHORT      |
      | maxLength | TOO_LONG       |
      | pattern   | INVALID_FORMAT |

  Scenario: Property without constraints carries no x-validations key
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "KitchenSinkDto" has property "description" without an x-validations extension

  Scenario Outline: Single-constraint DTO emits one x-validations entry per the mapping
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "<schema>" has property "value" with x-validations
      | rule   | code   |
      | <rule> | <code> |

    Examples:
      | schema             | rule             | code                      |
      | NotNullDto         | required         | REQUIRED                  |
      | NotEmptyStringDto  | minLength        | REQUIRED                  |
      | NotBlankDto        | notBlank         | BLANK                     |
      | NullDto            | null             | MUST_BE_NULL              |
      | MinDto             | minimum          | TOO_SMALL                 |
      | DecimalMinDto      | minimum          | TOO_SMALL                 |
      | MaxDto             | maximum          | TOO_LARGE                 |
      | DecimalMaxDto      | maximum          | TOO_LARGE                 |
      | PositiveDto        | exclusiveMinimum | MUST_BE_POSITIVE          |
      | PositiveOrZeroDto  | minimum          | MUST_BE_POSITIVE_OR_ZERO  |
      | NegativeDto        | exclusiveMaximum | MUST_BE_NEGATIVE          |
      | NegativeOrZeroDto  | maximum          | MUST_BE_NEGATIVE_OR_ZERO  |
      | DigitsDto          | digits           | INVALID_NUMBER_FORMAT     |
      | PatternDto         | pattern          | INVALID_FORMAT            |
      | EmailDto           | format           | INVALID_EMAIL             |
      | PastDto            | past             | MUST_BE_PAST              |
      | PastOrPresentDto   | pastOrPresent    | MUST_BE_PAST_OR_PRESENT   |
      | FutureDto          | future           | MUST_BE_FUTURE            |
      | FutureOrPresentDto | futureOrPresent  | MUST_BE_FUTURE_OR_PRESENT |
      | AssertTrueDto      | assertTrue       | MUST_BE_TRUE              |
      | AssertFalseDto     | assertFalse      | MUST_BE_FALSE             |

  Scenario: Size on a string emits both length entries when both bounds are set
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "SizeDto" has property "value" with x-validations
      | rule      | code      |
      | minLength | TOO_SHORT |
      | maxLength | TOO_LONG  |

  Scenario: Length emits both length entries when both bounds are set
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "LengthDto" has property "value" with x-validations
      | rule      | code      |
      | minLength | TOO_SHORT |
      | maxLength | TOO_LONG  |

  Scenario: Range with both bounds emits minimum and maximum
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "RangeDto" has property "value" with x-validations
      | rule    | code      |
      | minimum | TOO_SMALL |
      | maximum | TOO_LARGE |

  Scenario: Size on a collection emits item-count entries
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "SizeListDto" has property "items" with x-validations
      | rule     | code      |
      | minItems | TOO_SHORT |
      | maxItems | TOO_LONG  |

  Scenario: NotEmpty on a collection emits a minItems entry
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "NotEmptyListDto" has property "items" with x-validations
      | rule     | code     |
      | minItems | REQUIRED |

  Scenario: Size with only the upper bound set emits only the maxLength entry
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "SizeMaxOnlyDto" has property "value" with x-validations
      | rule      | code     |
      | maxLength | TOO_LONG |

  Scenario: Range with only the upper bound set still emits the minimum entry
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "RangeMaxOnlyDto" has property "value" with x-validations
      | rule    | code      |
      | minimum | TOO_SMALL |
      | maximum | TOO_LARGE |

  Scenario: Range with only the lower bound set emits only the minimum entry
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "RangeMinOnlyDto" has property "value" with x-validations
      | rule    | code      |
      | minimum | TOO_SMALL |

  Scenario: NotNull marks the property as required at the parent schema level
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "NotNullDto" has "value" in its required fields

  Scenario: NotEmpty on string emits minLength 1
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "NotEmptyStringDto" has property "value" with JSON Schema validators
      | key       | value |
      | minLength | 1     |

  Scenario: NotEmpty on collection emits minItems 1
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "NotEmptyListDto" has property "items" with JSON Schema validators
      | key      | value |
      | minItems | 1     |

  Scenario: Size on string emits minLength and maxLength
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "SizeDto" has property "value" with JSON Schema validators
      | key       | value |
      | minLength | 2     |
      | maxLength | 10    |

  Scenario: Size on collection emits minItems and maxItems
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "SizeListDto" has property "items" with JSON Schema validators
      | key      | value |
      | minItems | 1     |
      | maxItems | 5     |

  Scenario: Size with only the upper bound set emits only maxLength
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "SizeMaxOnlyDto" has property "value" with JSON Schema validators
      | key       | value |
      | maxLength | 10    |

  Scenario: Length emits minLength and maxLength
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "LengthDto" has property "value" with JSON Schema validators
      | key       | value |
      | minLength | 2     |
      | maxLength | 10    |

  Scenario: Min emits minimum
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "MinDto" has property "value" with JSON Schema validators
      | key     | value |
      | minimum | 5     |

  Scenario: Max emits maximum
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "MaxDto" has property "value" with JSON Schema validators
      | key     | value |
      | maximum | 10    |

  Scenario: DecimalMin emits minimum
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "DecimalMinDto" has property "value" with JSON Schema validators
      | key     | value |
      | minimum | 5.0   |

  Scenario: DecimalMax emits maximum
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "DecimalMaxDto" has property "value" with JSON Schema validators
      | key     | value |
      | maximum | 10.0  |

  Scenario: Range emits minimum and maximum
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "RangeDto" has property "value" with JSON Schema validators
      | key     | value |
      | minimum | 5     |
      | maximum | 10    |

  Scenario: Range with only upper bound set still emits minimum 0 from the @Range default
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "RangeMaxOnlyDto" has property "value" with JSON Schema validators
      | key     | value |
      | minimum | 0     |
      | maximum | 100   |

  Scenario: Range with only lower bound set emits minimum 5
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "RangeMinOnlyDto" has property "value" with JSON Schema validators
      | key     | value |
      | minimum | 5     |

  Scenario: Positive emits minimum 0 with exclusiveMinimum 0
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "PositiveDto" has property "value" with JSON Schema validators
      | key              | value |
      | minimum          | 0     |
      | exclusiveMinimum | 0     |

  Scenario: PositiveOrZero emits minimum 0
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "PositiveOrZeroDto" has property "value" with JSON Schema validators
      | key     | value |
      | minimum | 0     |

  Scenario: Negative emits maximum 0 with exclusiveMaximum 0
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "NegativeDto" has property "value" with JSON Schema validators
      | key              | value |
      | maximum          | 0     |
      | exclusiveMaximum | 0     |

  Scenario: NegativeOrZero emits maximum 0
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "NegativeOrZeroDto" has property "value" with JSON Schema validators
      | key     | value |
      | maximum | 0     |

  Scenario: Pattern emits the regexp as the JSON Schema pattern
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "PatternDto" has property "value" with JSON Schema validators
      | key     | value    |
      | pattern | "[A-Z]+" |

  Scenario: Email emits format email
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "EmailDto" has property "value" with JSON Schema validators
      | key    | value   |
      | format | "email" |
