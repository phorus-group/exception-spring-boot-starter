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

  Scenario: NotBlank marks the property as required at the parent schema level
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "NotBlankDto" has "value" in its required fields

  Scenario: NotEmpty on a string marks the property as required at the parent schema level
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "NotEmptyStringDto" has "value" in its required fields

  Scenario: NotEmpty on a collection marks the property as required at the parent schema level
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "NotEmptyListDto" has "items" in its required fields

  # Validation groups: when a DTO is referenced by controllers that pin different
  # @Validated groups, each operation's request body schema must carry only the
  # x-validations and required entries that apply to that group.

  Scenario: Constraints scoped to the Create group appear on the Create endpoint's body schema
    When the caller fetches the OpenAPI document
    Then the OpenAPI body schema for POST "/v1/testGroupsCreate" has property "name" with x-validations
      | rule      | code     |
      | notBlank  | BLANK    |
      | maxLength | TOO_LONG |

  Scenario: Constraints scoped to other groups do not appear on the Update endpoint's body schema
    When the caller fetches the OpenAPI document
    Then the OpenAPI body schema for POST "/v1/testGroupsUpdate" has property "name" with x-validations
      | rule      | code     |
      | maxLength | TOO_LONG |
    And the OpenAPI body schema for POST "/v1/testGroupsUpdate" has property "name" without x-validations rule "notBlank"

  Scenario: notBlank on the Create group derives required for the Create endpoint
    When the caller fetches the OpenAPI document
    Then the OpenAPI body schema for POST "/v1/testGroupsCreate" has "name" in its required fields

  Scenario: notBlank on the Create group does not derive required for the Update endpoint
    When the caller fetches the OpenAPI document
    Then the OpenAPI body schema for POST "/v1/testGroupsUpdate" does not have "name" in its required fields

  # No-groups baseline: when constraint annotations omit `groups`, the same DTO is
  # shared by every operation that references it. The customizer must not clone, not
  # filter, and not drop the constraints.

  Scenario: A DTO without group-scoped constraints emits its constraints to every consumer
    When the caller fetches the OpenAPI document
    Then the OpenAPI body schema for POST "/v1/testUngroupedA" has property "name" with x-validations
      | rule      | code     |
      | notBlank  | BLANK    |
      | maxLength | TOO_LONG |
    And the OpenAPI body schema for POST "/v1/testUngroupedB" has property "name" with x-validations
      | rule      | code     |
      | notBlank  | BLANK    |
      | maxLength | TOO_LONG |

  Scenario: A DTO without group-scoped constraints still derives required from notBlank for every consumer
    When the caller fetches the OpenAPI document
    Then the OpenAPI body schema for POST "/v1/testUngroupedA" has "name" in its required fields
    And the OpenAPI body schema for POST "/v1/testUngroupedB" has "name" in its required fields

  # Method-level @Validated: Spring 6.1+ honors @Validated placed on the controller
  # method, not on the @RequestBody parameter. Same group-scoped clone behavior.

  Scenario: Method-level @Validated(Create) on the method clones the body schema for the Create group
    When the caller fetches the OpenAPI document
    Then the OpenAPI body schema for POST "/v1/testGroupsCreateMethodLevel" has property "name" with x-validations
      | rule      | code     |
      | notBlank  | BLANK    |
      | maxLength | TOO_LONG |
    And the OpenAPI body schema for POST "/v1/testGroupsCreateMethodLevel" has "name" in its required fields

  Scenario: Method-level @Validated(Update) on the method clones the body schema for the Update group
    When the caller fetches the OpenAPI document
    Then the OpenAPI body schema for POST "/v1/testGroupsUpdateMethodLevel" has property "name" with x-validations
      | rule      | code     |
      | maxLength | TOO_LONG |
    And the OpenAPI body schema for POST "/v1/testGroupsUpdateMethodLevel" does not have "name" in its required fields

  # Nested cascade: when the outer DTO is cloned for a group, every nested @Valid
  # field must point at a per-group clone of the inner DTO too. JSR 380 §5.4.5
  # propagates the active group through cascading, so the inner constraints
  # filter the same way.

  Scenario: Nested cascade emits a per-group clone of the inner DTO for the Create endpoint
    When the caller fetches the OpenAPI document
    Then the OpenAPI body schema for POST "/v1/testNestedGroupsCreate" has property "name" with x-validations
      | rule      | code     |
      | notBlank  | BLANK    |
      | maxLength | TOO_LONG |
    And the OpenAPI body schema for POST "/v1/testNestedGroupsCreate" has "name" in its required fields
    And the OpenAPI nested body schema for POST "/v1/testNestedGroupsCreate" property "inner" has property "email" with x-validations
      | rule     | code  |
      | notBlank | BLANK |
    And the OpenAPI nested body schema for POST "/v1/testNestedGroupsCreate" property "inner" has "email" in its required fields
    And the OpenAPI nested body schema for POST "/v1/testNestedGroupsCreate" property "inner" does not have "displayName" in its required fields

  Scenario: Nested cascade emits a per-group clone of the inner DTO for the Update endpoint
    When the caller fetches the OpenAPI document
    Then the OpenAPI body schema for POST "/v1/testNestedGroupsUpdate" has property "name" with x-validations
      | rule      | code     |
      | maxLength | TOO_LONG |
    And the OpenAPI nested body schema for POST "/v1/testNestedGroupsUpdate" property "inner" has property "displayName" with x-validations
      | rule     | code  |
      | notBlank | BLANK |
    And the OpenAPI nested body schema for POST "/v1/testNestedGroupsUpdate" property "inner" has "displayName" in its required fields
    And the OpenAPI nested body schema for POST "/v1/testNestedGroupsUpdate" property "inner" does not have "email" in its required fields

  # Parameter-level x-validations: path / query / header / cookie parameters carry
  # the same {rule, code} mapping as body DTO fields. Standard JSON Schema validators
  # (minLength, pattern, etc.) are still emitted by springdoc natively next to the
  # x-validations array.

  Scenario: @PathVariable with @NotBlank and @Size emits x-validations on the parameter
    When the caller fetches the OpenAPI document
    Then the OpenAPI parameter "id" for POST "/v1/testPathParam/{id}" has x-validations
      | rule      | code     |
      | notBlank  | BLANK    |
      | maxLength | TOO_LONG |

  Scenario: @RequestHeader with @NotBlank emits x-validations on the parameter
    When the caller fetches the OpenAPI document
    Then the OpenAPI parameter "X-Trace" for POST "/v1/testHeaderParam" has x-validations
      | rule     | code  |
      | notBlank | BLANK |

  Scenario: @CookieValue with @NotBlank emits x-validations on the parameter
    When the caller fetches the OpenAPI document
    Then the OpenAPI parameter "session" for POST "/v1/testCookieParam" has x-validations
      | rule     | code  |
      | notBlank | BLANK |

  Scenario: @RequestParam with @Pattern emits x-validations on the parameter
    When the caller fetches the OpenAPI document
    Then the OpenAPI parameter "code" for POST "/v1/testParamPattern" has x-validations
      | rule    | code           |
      | pattern | INVALID_FORMAT |

  Scenario: @RequestParam with @Min emits x-validations on the parameter
    When the caller fetches the OpenAPI document
    Then the OpenAPI parameter "limit" for POST "/v1/testParamMin" has x-validations
      | rule    | code      |
      | minimum | TOO_SMALL |

  Scenario: Parameters without constraints carry no x-validations key
    When the caller fetches the OpenAPI document
    Then the OpenAPI parameter "name" for POST "/v1/testParamPlain" has no x-validations

  Scenario: @Validated with multiple groups produces a single combined clone
    When the caller fetches the OpenAPI document
    Then the OpenAPI body schema for POST "/v1/testMultipleGroups" has property "name" with x-validations
      | rule      | code     |
      | notBlank  | BLANK    |
      | maxLength | TOO_LONG |
    And the OpenAPI body schema for POST "/v1/testMultipleGroups" has "name" in its required fields

  Scenario: Three-level cascade clones every nested component per active group
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "Level1Dto_CreateGroup" has "name" in its required fields
    And the OpenAPI schema "Level2Dto_CreateGroup" has "name" in its required fields
    And the OpenAPI schema "Level3Dto_CreateGroup" has "name" in its required fields
    And the OpenAPI schema "Level1Dto_CreateGroup" property "level2" references "Level2Dto_CreateGroup"
    And the OpenAPI schema "Level2Dto_CreateGroup" property "level3" references "Level3Dto_CreateGroup"

  Scenario: Outer is cloned but inner without group-scoped constraints keeps the original ref
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "OuterWithPlainInnerDto_CreateGroup" has "name" in its required fields
    And the OpenAPI schema "OuterWithPlainInnerDto_CreateGroup" property "plain" references "PlainInnerDto"

  Scenario: Parameter dedupes constraints that produce the same rule
    When the caller fetches the OpenAPI document
    Then the OpenAPI parameter "limit" for POST "/v1/testParamDedup" has x-validations
      | rule    | code                     |
      | minimum | MUST_BE_POSITIVE_OR_ZERO |

  Scenario: Parameter x-validations entries do not leak the internal groups key
    When the caller fetches the OpenAPI document
    Then the OpenAPI parameter "limit" for POST "/v1/testParamMin" x-validations entries carry no internal keys

  Scenario: GroupsDto is registered alongside its per-group clones as three distinct components
    When the caller fetches the OpenAPI document
    Then the OpenAPI document declares schema "GroupsDto"
    And the OpenAPI document declares schema "GroupsDto_CreateGroup"
    And the OpenAPI document declares schema "GroupsDto_UpdateGroup"

  Scenario: InnerGroupsDto is registered alongside its per-group clones as three distinct components
    When the caller fetches the OpenAPI document
    Then the OpenAPI document declares schema "InnerGroupsDto"
    And the OpenAPI document declares schema "InnerGroupsDto_CreateGroup"
    And the OpenAPI document declares schema "InnerGroupsDto_UpdateGroup"

  Scenario: Create clone carries only the Create-group entries on its name property
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupsDto_CreateGroup" has property "name" with x-validations
      | rule      | code     |
      | notBlank  | BLANK    |
      | maxLength | TOO_LONG |
    And the OpenAPI schema "GroupsDto_CreateGroup" has "name" in its required fields

  Scenario: Update clone carries only the Update-group entries on its name property
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupsDto_UpdateGroup" has property "name" with x-validations
      | rule      | code     |
      | maxLength | TOO_LONG |
    And the OpenAPI document does not declare schema "GroupsDto_UpdateGroup_CreateGroup"

  Scenario: Inner Create clone carries only Create-group entries
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "InnerGroupsDto_CreateGroup" has property "email" with x-validations
      | rule     | code  |
      | notBlank | BLANK |
    And the OpenAPI schema "InnerGroupsDto_CreateGroup" has "email" in its required fields

  Scenario: Inner Update clone carries only Update-group entries
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "InnerGroupsDto_UpdateGroup" has property "displayName" with x-validations
      | rule     | code  |
      | notBlank | BLANK |
    And the OpenAPI schema "InnerGroupsDto_UpdateGroup" has "displayName" in its required fields

  # Default-group view of the original component.
  # @Valid (no @Validated) runs the Default group at runtime, so group-scoped constraints
  # do not fire. The original component is filtered to match that contract: only entries
  # whose `groups()` attribute is empty survive on the served original.

  Scenario: Original GroupsDto carries no x-validations on a field whose constraints are all group-scoped
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupsDto" has property "name" without an x-validations extension

  Scenario: @Valid endpoint on a group-scoped DTO references the filtered original
    When the caller fetches the OpenAPI document
    Then the OpenAPI body schema for POST "/v1/testValidOnGroupedDto" has property "name" without an x-validations extension
    And the OpenAPI body schema for POST "/v1/testValidOnGroupedDto" does not have "name" in its required fields

  Scenario: @Validated with no group references the filtered original same as @Valid
    When the caller fetches the OpenAPI document
    Then the OpenAPI body schema for POST "/v1/testValidatedNoGroupOnGroupedDto" has property "name" without an x-validations extension
    And the OpenAPI body schema for POST "/v1/testValidatedNoGroupOnGroupedDto" does not have "name" in its required fields

  Scenario: Original of a DTO with mixed ungrouped and group-scoped constraints keeps only the ungrouped entry
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "MixedConstraintsDto" has property "name" with x-validations
      | rule     | code  |
      | notBlank | BLANK |
    And the OpenAPI schema "MixedConstraintsDto" has "name" in its required fields

  Scenario: @Valid endpoint on a mixed DTO references the filtered original
    When the caller fetches the OpenAPI document
    Then the OpenAPI body schema for POST "/v1/testValidOnMixed" has property "name" with x-validations
      | rule     | code  |
      | notBlank | BLANK |

  Scenario: Original nested inner DTO is filtered to its default view when reached through @Valid
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "InnerGroupsDto" has property "email" without an x-validations extension
    And the OpenAPI schema "InnerGroupsDto" has property "displayName" without an x-validations extension

  # Orphan pruning. A component referenced by no operation and no other component is
  # dropped from `components.schemas`.

  Scenario: A DTO consumed only via @Validated(Group) leaves no original component in the spec
    When the caller fetches the OpenAPI document
    Then the OpenAPI document declares schema "OnlyGroupsOrphanDto_CreateGroup"
    And the OpenAPI document does not declare schema "OnlyGroupsOrphanDto"

  Scenario: A DTO used only as a response body is not pruned and stays referenced by its response
    When the caller fetches the OpenAPI document
    Then the OpenAPI document declares schema "ResponseOnlyDto"

  # JSON Schema validators (minLength, maxLength, pattern, minimum, ...) emitted by
  # springdoc from a constraint annotation must follow the same group filtering as the
  # `x-validations` array. Otherwise an FE codegen reading the spec would emit a
  # validator the BE does not actually enforce under that group.

  Scenario: Default-view of MixedConstraintsDto drops the pattern set by the Create-scoped annotation
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "MixedConstraintsDto" has property "name" without JSON Schema validators
      | key     |
      | pattern |

  Scenario: Default-view of OuterGroupsDto drops the maxLength set by the group-scoped annotation
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "OuterGroupsDto" has property "name" without JSON Schema validators
      | key       |
      | maxLength |

  Scenario: Per-group clone keeps the maxLength when the source annotation matches the active group
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupsDto_CreateGroup" has property "name" with JSON Schema validators
      | key       | value |
      | maxLength | 100   |
    And the OpenAPI schema "GroupsDto_UpdateGroup" has property "name" with JSON Schema validators
      | key       | value |
      | maxLength | 100   |

  # Collection-element cascade: `List<@Valid Inner>` puts the inner $ref under `items.$ref`,
  # not under `properties.$ref`. The cloning walker must follow that path too.

  Scenario: Collection-element cascade clones the inner DTO referenced via items.$ref
    When the caller fetches the OpenAPI document
    Then the OpenAPI document declares schema "CollectionGroupsDto_CreateGroup"
    And the OpenAPI document declares schema "InnerGroupsDto_CreateGroup"
    And the OpenAPI schema "CollectionGroupsDto_CreateGroup" items property "items" references "InnerGroupsDto_CreateGroup"

  # Exhaustive group-scoped JSON Schema validator coverage. One field per Jakarta /
  # Hibernate annotation that produces a JSON Schema validator, every constraint scoped
  # to CreateGroup. The same DTO is consumed via @Validated(Create) (per-group clone)
  # and via @Valid (default-view original), so each annotation is asserted both ways.

  Scenario: Grouped @NotEmpty on string emits minLength on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDto_CreateGroup" has property "notEmptyString" with JSON Schema validators
      | key       | value |
      | minLength | 1     |
    And the OpenAPI schema "GroupedValidatorsDto" has property "notEmptyString" without JSON Schema validators
      | key       |
      | minLength |

  Scenario: Grouped @NotEmpty on collection emits minItems on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDto_CreateGroup" has property "notEmptyList" with JSON Schema validators
      | key      | value |
      | minItems | 1     |
    And the OpenAPI schema "GroupedValidatorsDto" has property "notEmptyList" without JSON Schema validators
      | key      |
      | minItems |

  Scenario: Grouped @Size on string emits minLength + maxLength on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDto_CreateGroup" has property "sizeString" with JSON Schema validators
      | key       | value |
      | minLength | 2     |
      | maxLength | 50    |
    And the OpenAPI schema "GroupedValidatorsDto" has property "sizeString" without JSON Schema validators
      | key       |
      | minLength |
      | maxLength |

  Scenario: Grouped @Size on collection emits minItems + maxItems on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDto_CreateGroup" has property "sizeList" with JSON Schema validators
      | key      | value |
      | minItems | 2     |
      | maxItems | 50    |
    And the OpenAPI schema "GroupedValidatorsDto" has property "sizeList" without JSON Schema validators
      | key      |
      | minItems |
      | maxItems |

  Scenario: Grouped @Length emits minLength + maxLength on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDto_CreateGroup" has property "lengthString" with JSON Schema validators
      | key       | value |
      | minLength | 2     |
      | maxLength | 50    |
    And the OpenAPI schema "GroupedValidatorsDto" has property "lengthString" without JSON Schema validators
      | key       |
      | minLength |
      | maxLength |

  Scenario: Grouped @Min emits minimum on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDto_CreateGroup" has property "minInt" with JSON Schema validators
      | key     | value |
      | minimum | 5     |
    And the OpenAPI schema "GroupedValidatorsDto" has property "minInt" without JSON Schema validators
      | key     |
      | minimum |

  Scenario: Grouped @Max emits maximum on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDto_CreateGroup" has property "maxInt" with JSON Schema validators
      | key     | value |
      | maximum | 50    |
    And the OpenAPI schema "GroupedValidatorsDto" has property "maxInt" without JSON Schema validators
      | key     |
      | maximum |

  Scenario: Grouped @DecimalMin emits minimum on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDto_CreateGroup" has property "decimalMin" with JSON Schema validators
      | key     | value |
      | minimum | 5.5   |
    And the OpenAPI schema "GroupedValidatorsDto" has property "decimalMin" without JSON Schema validators
      | key     |
      | minimum |

  Scenario: Grouped @DecimalMax emits maximum on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDto_CreateGroup" has property "decimalMax" with JSON Schema validators
      | key     | value |
      | maximum | 50.5  |
    And the OpenAPI schema "GroupedValidatorsDto" has property "decimalMax" without JSON Schema validators
      | key     |
      | maximum |

  Scenario: Grouped @Range emits minimum + maximum on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDto_CreateGroup" has property "rangeInt" with JSON Schema validators
      | key     | value |
      | minimum | 5     |
      | maximum | 50    |
    And the OpenAPI schema "GroupedValidatorsDto" has property "rangeInt" without JSON Schema validators
      | key     |
      | minimum |
      | maximum |

  Scenario: Grouped @Positive emits minimum + exclusiveMinimum on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDto_CreateGroup" has property "positiveInt" with JSON Schema validators
      | key              | value |
      | minimum          | 0     |
      | exclusiveMinimum | 0     |
    And the OpenAPI schema "GroupedValidatorsDto" has property "positiveInt" without JSON Schema validators
      | key              |
      | minimum          |
      | exclusiveMinimum |

  Scenario: Grouped @PositiveOrZero emits minimum on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDto_CreateGroup" has property "positiveOrZeroInt" with JSON Schema validators
      | key     | value |
      | minimum | 0     |
    And the OpenAPI schema "GroupedValidatorsDto" has property "positiveOrZeroInt" without JSON Schema validators
      | key     |
      | minimum |

  Scenario: Grouped @Negative emits maximum + exclusiveMaximum on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDto_CreateGroup" has property "negativeInt" with JSON Schema validators
      | key              | value |
      | maximum          | 0     |
      | exclusiveMaximum | 0     |
    And the OpenAPI schema "GroupedValidatorsDto" has property "negativeInt" without JSON Schema validators
      | key              |
      | maximum          |
      | exclusiveMaximum |

  Scenario: Grouped @NegativeOrZero emits maximum on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDto_CreateGroup" has property "negativeOrZeroInt" with JSON Schema validators
      | key     | value |
      | maximum | 0     |
    And the OpenAPI schema "GroupedValidatorsDto" has property "negativeOrZeroInt" without JSON Schema validators
      | key     |
      | maximum |

  Scenario: Grouped @Pattern emits pattern on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDto_CreateGroup" has property "patternString" with JSON Schema validators
      | key     | value    |
      | pattern | "[A-Z]+" |
    And the OpenAPI schema "GroupedValidatorsDto" has property "patternString" without JSON Schema validators
      | key     |
      | pattern |

  Scenario: Grouped @Email emits format on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDto_CreateGroup" has property "emailString" with JSON Schema validators
      | key    | value   |
      | format | "email" |
    And the OpenAPI schema "GroupedValidatorsDto" has property "emailString" without JSON Schema validators
      | key    |
      | format |

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
