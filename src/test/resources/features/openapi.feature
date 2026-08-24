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
    Then the OpenAPI schema "Level1DtoCreateGroup" has "name" in its required fields
    And the OpenAPI schema "Level2DtoCreateGroup" has "name" in its required fields
    And the OpenAPI schema "Level3DtoCreateGroup" has "name" in its required fields
    And the OpenAPI schema "Level1DtoCreateGroup" property "level2" references "Level2DtoCreateGroup"
    And the OpenAPI schema "Level2DtoCreateGroup" property "level3" references "Level3DtoCreateGroup"

  Scenario: Outer is cloned but inner without group-scoped constraints keeps the original ref
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "OuterWithPlainInnerDtoCreateGroup" has "name" in its required fields
    And the OpenAPI schema "OuterWithPlainInnerDtoCreateGroup" property "plain" references "PlainInnerDto"

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
    And the OpenAPI document declares schema "GroupsDtoCreateGroup"
    And the OpenAPI document declares schema "GroupsDtoUpdateGroup"

  Scenario: InnerGroupsDto is registered alongside its per-group clones as three distinct components
    When the caller fetches the OpenAPI document
    Then the OpenAPI document declares schema "InnerGroupsDto"
    And the OpenAPI document declares schema "InnerGroupsDtoCreateGroup"
    And the OpenAPI document declares schema "InnerGroupsDtoUpdateGroup"

  Scenario: Create clone carries only the Create-group entries on its name property
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupsDtoCreateGroup" has property "name" with x-validations
      | rule      | code     |
      | notBlank  | BLANK    |
      | maxLength | TOO_LONG |
    And the OpenAPI schema "GroupsDtoCreateGroup" has "name" in its required fields

  Scenario: Update clone carries only the Update-group entries on its name property
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupsDtoUpdateGroup" has property "name" with x-validations
      | rule      | code     |
      | maxLength | TOO_LONG |
    And the OpenAPI document does not declare schema "GroupsDtoUpdateGroupCreateGroup"

  Scenario: Inner Create clone carries only Create-group entries
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "InnerGroupsDtoCreateGroup" has property "email" with x-validations
      | rule     | code  |
      | notBlank | BLANK |
    And the OpenAPI schema "InnerGroupsDtoCreateGroup" has "email" in its required fields

  Scenario: Inner Update clone carries only Update-group entries
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "InnerGroupsDtoUpdateGroup" has property "displayName" with x-validations
      | rule     | code  |
      | notBlank | BLANK |
    And the OpenAPI schema "InnerGroupsDtoUpdateGroup" has "displayName" in its required fields

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
    Then the OpenAPI document declares schema "OnlyGroupsOrphanDtoCreateGroup"
    And the OpenAPI document does not declare schema "OnlyGroupsOrphanDto"

  Scenario: A DTO used only as a response body is not pruned and stays referenced by its response
    When the caller fetches the OpenAPI document
    Then the OpenAPI document declares schema "ResponseOnlyDto"

  # ApiError and ValidationError are registered manually via ModelConverters.read, which
  # bypasses springdoc nullability-driven required derivation. The customizer must set
  # required[] explicitly so consumers can rely on non-nullable Kotlin fields actually
  # arriving on every error response.

  Scenario: ApiError schema marks every non-nullable field as required
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "ApiError" has "status" in its required fields
    And the OpenAPI schema "ApiError" has "title" in its required fields
    And the OpenAPI schema "ApiError" has "code" in its required fields
    And the OpenAPI schema "ApiError" has "timestamp" in its required fields
    And the OpenAPI schema "ApiError" does not have "detail" in its required fields
    And the OpenAPI schema "ApiError" does not have "source" in its required fields
    And the OpenAPI schema "ApiError" does not have "metadata" in its required fields
    And the OpenAPI schema "ApiError" does not have "validationErrors" in its required fields

  # JSON Schema `type` must survive group cloning. The original schema springdoc emits
  # carries `type: string` / `type: integer` etc.; the cloned per-group component must
  # carry the same. Drift here breaks SDK generators that read the spec.

  Scenario: Cloned schema preserves the property type
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupsDto" property "name" has type "string"
    And the OpenAPI schema "GroupsDtoCreateGroup" property "name" has type "string"
    And the OpenAPI schema "GroupsDtoUpdateGroup" property "name" has type "string"

  Scenario: Cloned schema preserves the integer property type
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDtoCreateGroup" property "minInt" has type "integer"
    And the OpenAPI schema "GroupedValidatorsDtoCreateGroup" property "minInt" property "format" equals "int32"

  Scenario: Cloned schema preserves UUID format
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedRichDtoCreateGroup" property "id" has type "string"
    And the OpenAPI schema "GroupedRichDtoCreateGroup" property "id" property "format" equals "uuid"

  Scenario: Cloned schema preserves @Schema description, title, and example
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedRichDtoCreateGroup" property "name" property "description" equals "Display name"
    And the OpenAPI schema "GroupedRichDtoCreateGroup" property "name" property "title" equals "Name"
    And the OpenAPI schema "GroupedRichDtoCreateGroup" property "name" property "example" equals "Alice"

  Scenario: Cloned schema preserves enum values
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedRichDtoCreateGroup" property "status" has enum values "ACTIVE,INACTIVE"

  Scenario: Cloned schema preserves default values
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedRichDtoCreateGroup" property "status" property "default" equals "ACTIVE"

  # Class-level @Validated(Group): Spring's method-validation interceptor falls back to
  # the class-level @Validated value when no parameter-level or method-level @Validated
  # is present. The customizer reads class-level too so the OpenAPI document matches
  # what Spring actually enforces at runtime.

  Scenario: Class-level @Validated(Create) on the controller clones the body schema for the Create group
    When the caller fetches the OpenAPI document
    Then the OpenAPI body schema for POST "/v1/testClassLevelValidatedCreate" has property "name" with x-validations
      | rule      | code     |
      | notBlank  | BLANK    |
      | maxLength | TOO_LONG |
    And the OpenAPI body schema for POST "/v1/testClassLevelValidatedCreate" has "name" in its required fields

  # Multi media-type request bodies: a controller method declaring multiple `consumes`
  # values gets one schema entry per media type. The customizer rewrites every entry to
  # point at the per-group clone, not just the first.

  Scenario: Multi-media-type body schema is cloned for every declared content type
    When the caller fetches the OpenAPI document
    Then the OpenAPI body schema for POST "/v1/testMultiMediaCreate" content type "application/json" references "GroupsDtoCreateGroup"
    And the OpenAPI body schema for POST "/v1/testMultiMediaCreate" content type "application/xml" references "GroupsDtoCreateGroup"

  Scenario: ValidationError schema marks the non-nullable obj field as required
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "ValidationError" has "obj" in its required fields
    And the OpenAPI schema "ValidationError" does not have "field" in its required fields
    And the OpenAPI schema "ValidationError" does not have "code" in its required fields
    And the OpenAPI schema "ValidationError" does not have "rejectedValue" in its required fields
    And the OpenAPI schema "ValidationError" does not have "message" in its required fields
    And the OpenAPI schema "ValidationError" does not have "metadata" in its required fields

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
    Then the OpenAPI schema "GroupsDtoCreateGroup" has property "name" with JSON Schema validators
      | key       | value |
      | maxLength | 100   |
    And the OpenAPI schema "GroupsDtoUpdateGroup" has property "name" with JSON Schema validators
      | key       | value |
      | maxLength | 100   |

  # Collection-element cascade: `List<@Valid Inner>` puts the inner $ref under `items.$ref`,
  # not under `properties.$ref`. The cloning walker must follow that path too.

  Scenario: Collection-element cascade clones the inner DTO referenced via items.$ref
    When the caller fetches the OpenAPI document
    Then the OpenAPI document declares schema "CollectionGroupsDtoCreateGroup"
    And the OpenAPI document declares schema "InnerGroupsDtoCreateGroup"
    And the OpenAPI schema "CollectionGroupsDtoCreateGroup" items property "items" references "InnerGroupsDtoCreateGroup"

  # Exhaustive group-scoped JSON Schema validator coverage. One field per Jakarta /
  # Hibernate annotation that produces a JSON Schema validator, every constraint scoped
  # to CreateGroup. The same DTO is consumed via @Validated(Create) (per-group clone)
  # and via @Valid (default-view original), so each annotation is asserted both ways.

  Scenario: Grouped @NotEmpty on string emits minLength on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDtoCreateGroup" has property "notEmptyString" with JSON Schema validators
      | key       | value |
      | minLength | 1     |
    And the OpenAPI schema "GroupedValidatorsDto" has property "notEmptyString" without JSON Schema validators
      | key       |
      | minLength |

  Scenario: Grouped @NotEmpty on collection emits minItems on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDtoCreateGroup" has property "notEmptyList" with JSON Schema validators
      | key      | value |
      | minItems | 1     |
    And the OpenAPI schema "GroupedValidatorsDto" has property "notEmptyList" without JSON Schema validators
      | key      |
      | minItems |

  Scenario: Grouped @Size on string emits minLength + maxLength on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDtoCreateGroup" has property "sizeString" with JSON Schema validators
      | key       | value |
      | minLength | 2     |
      | maxLength | 50    |
    And the OpenAPI schema "GroupedValidatorsDto" has property "sizeString" without JSON Schema validators
      | key       |
      | minLength |
      | maxLength |

  Scenario: Grouped @Size on collection emits minItems + maxItems on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDtoCreateGroup" has property "sizeList" with JSON Schema validators
      | key      | value |
      | minItems | 2     |
      | maxItems | 50    |
    And the OpenAPI schema "GroupedValidatorsDto" has property "sizeList" without JSON Schema validators
      | key      |
      | minItems |
      | maxItems |

  Scenario: Grouped @Length emits minLength + maxLength on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDtoCreateGroup" has property "lengthString" with JSON Schema validators
      | key       | value |
      | minLength | 2     |
      | maxLength | 50    |
    And the OpenAPI schema "GroupedValidatorsDto" has property "lengthString" without JSON Schema validators
      | key       |
      | minLength |
      | maxLength |

  Scenario: Grouped @Min emits minimum on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDtoCreateGroup" has property "minInt" with JSON Schema validators
      | key     | value |
      | minimum | 5     |
    And the OpenAPI schema "GroupedValidatorsDto" has property "minInt" without JSON Schema validators
      | key     |
      | minimum |

  Scenario: Grouped @Max emits maximum on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDtoCreateGroup" has property "maxInt" with JSON Schema validators
      | key     | value |
      | maximum | 50    |
    And the OpenAPI schema "GroupedValidatorsDto" has property "maxInt" without JSON Schema validators
      | key     |
      | maximum |

  Scenario: Grouped @DecimalMin emits minimum on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDtoCreateGroup" has property "decimalMin" with JSON Schema validators
      | key     | value |
      | minimum | 5.5   |
    And the OpenAPI schema "GroupedValidatorsDto" has property "decimalMin" without JSON Schema validators
      | key     |
      | minimum |

  Scenario: Grouped @DecimalMax emits maximum on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDtoCreateGroup" has property "decimalMax" with JSON Schema validators
      | key     | value |
      | maximum | 50.5  |
    And the OpenAPI schema "GroupedValidatorsDto" has property "decimalMax" without JSON Schema validators
      | key     |
      | maximum |

  Scenario: Grouped @Range emits minimum + maximum on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDtoCreateGroup" has property "rangeInt" with JSON Schema validators
      | key     | value |
      | minimum | 5     |
      | maximum | 50    |
    And the OpenAPI schema "GroupedValidatorsDto" has property "rangeInt" without JSON Schema validators
      | key     |
      | minimum |
      | maximum |

  Scenario: Grouped @Positive emits minimum + exclusiveMinimum on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDtoCreateGroup" has property "positiveInt" with JSON Schema validators
      | key              | value |
      | minimum          | 0     |
      | exclusiveMinimum | 0     |
    And the OpenAPI schema "GroupedValidatorsDto" has property "positiveInt" without JSON Schema validators
      | key              |
      | minimum          |
      | exclusiveMinimum |

  Scenario: Grouped @PositiveOrZero emits minimum on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDtoCreateGroup" has property "positiveOrZeroInt" with JSON Schema validators
      | key     | value |
      | minimum | 0     |
    And the OpenAPI schema "GroupedValidatorsDto" has property "positiveOrZeroInt" without JSON Schema validators
      | key     |
      | minimum |

  Scenario: Grouped @Negative emits maximum + exclusiveMaximum on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDtoCreateGroup" has property "negativeInt" with JSON Schema validators
      | key              | value |
      | maximum          | 0     |
      | exclusiveMaximum | 0     |
    And the OpenAPI schema "GroupedValidatorsDto" has property "negativeInt" without JSON Schema validators
      | key              |
      | maximum          |
      | exclusiveMaximum |

  Scenario: Grouped @NegativeOrZero emits maximum on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDtoCreateGroup" has property "negativeOrZeroInt" with JSON Schema validators
      | key     | value |
      | maximum | 0     |
    And the OpenAPI schema "GroupedValidatorsDto" has property "negativeOrZeroInt" without JSON Schema validators
      | key     |
      | maximum |

  Scenario: Grouped @Pattern emits pattern on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDtoCreateGroup" has property "patternString" with JSON Schema validators
      | key     | value    |
      | pattern | "[A-Z]+" |
    And the OpenAPI schema "GroupedValidatorsDto" has property "patternString" without JSON Schema validators
      | key     |
      | pattern |

  Scenario: Grouped @Email emits format on the clone and nothing on the default view
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupedValidatorsDtoCreateGroup" has property "emailString" with JSON Schema validators
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

  # A group pinned on a multipart `@RequestPart` is enforced by Spring exactly as one pinned
  # on a `@RequestBody`. The per-group clone must be derived for both, otherwise the published
  # contract understates what the server rejects for that operation. The object part sits
  # alongside binary parts that carry no pin of their own.

  Scenario: Request part pinned to a group gets the per-group clone
    When the caller fetches the OpenAPI document
    Then the OpenAPI document declares schema "MultipartPartDtoCreateGroup"

  Scenario: Request part group clone carries the group-scoped constraint
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "MultipartPartDtoCreateGroup" has property "name" with x-validations
      | rule     | code  |
      | notBlank | BLANK |
    And the OpenAPI schema "MultipartPartDtoCreateGroup" has "name" in its required fields

  Scenario: Multipart body points its object part at the per-group clone
    When the caller fetches the OpenAPI document
    Then the OpenAPI multipart body schema for POST "/v1/testMultipartCreate" part "data" references "MultipartPartDtoCreateGroup"

  # One DTO reached from a multipart create and a JSON update, the shape a CRUD resource takes.
  # Each operation must see only its own group's constraints.

  Scenario: A DTO shared by a multipart create and a JSON update gets both clones
    When the caller fetches the OpenAPI document
    Then the OpenAPI document declares schema "MultipartSharedDtoCreateGroup"
    And the OpenAPI document declares schema "MultipartSharedDtoUpdateGroup"

  Scenario: Each shared clone carries only its own group's constraints
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "MultipartSharedDtoCreateGroup" has "name" in its required fields
    And the OpenAPI schema "MultipartSharedDtoCreateGroup" does not have "description" in its required fields
    And the OpenAPI schema "MultipartSharedDtoUpdateGroup" has property "description" with x-validations
      | rule      | code     |
      | maxLength | TOO_LONG |
    And the OpenAPI schema "MultipartSharedDtoUpdateGroup" has property "name" without an x-validations extension

  Scenario: Multipart create and JSON update each point at their own clone
    When the caller fetches the OpenAPI document
    Then the OpenAPI multipart body schema for POST "/v1/testMultipartSharedCreate" part "data" references "MultipartSharedDtoCreateGroup"
    And the OpenAPI body schema for POST "/v1/testMultipartSharedUpdate" content type "application/json" references "MultipartSharedDtoUpdateGroup"

  # An unpinned part must behave like an unpinned body: no clone, original keeps its constraints.

  Scenario: Ungrouped request part derives no clone
    When the caller fetches the OpenAPI document
    Then the OpenAPI document does not declare schema "MultipartUngroupedDtoCreateGroup"
    And the OpenAPI schema "MultipartUngroupedDto" has property "value" with x-validations
      | rule     | code  |
      | notBlank | BLANK |

  # The pin may live on the method rather than the parameter, as it already may for a body.

  Scenario: Method-level group pin applies to a request part
    When the caller fetches the OpenAPI document
    Then the OpenAPI document declares schema "MultipartMethodLevelDtoCreateGroup"
    And the OpenAPI multipart body schema for POST "/v1/testMultipartMethodLevel" part "data" references "MultipartMethodLevelDtoCreateGroup"

  # A part cascades into nested components exactly as a body does.

  Scenario: A DTO cascaded from a request part is cloned per group
    When the caller fetches the OpenAPI document
    Then the OpenAPI document declares schema "MultipartOuterDtoCreateGroup"
    And the OpenAPI document declares schema "MultipartInnerDtoCreateGroup"

  Scenario: The nested clone reached from a part carries only its group's entries
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "MultipartInnerDtoCreateGroup" has property "email" with x-validations
      | rule     | code  |
      | notBlank | BLANK |
    And the OpenAPI schema "MultipartInnerDtoCreateGroup" has property "displayName" without an x-validations extension

  # Several groups pinned on one part produce a single combined clone, as they do for a body.

  Scenario: A request part pinned to multiple groups produces one combined clone
    When the caller fetches the OpenAPI document
    Then the OpenAPI multipart body schema for POST "/v1/testMultipartMultiGroup" part "data" has property "name" with x-validations
      | rule      | code     |
      | notBlank  | BLANK    |
      | maxLength | TOO_LONG |

  # Once a part's clone exists the original has no consumer and is pruned, as for a grouped body.

  Scenario: A DTO consumed only via a grouped request part leaves no original component
    When the caller fetches the OpenAPI document
    Then the OpenAPI document declares schema "MultipartOrphanDtoCreateGroup"
    And the OpenAPI document does not declare schema "MultipartOrphanDto"

  # Two object parts on one operation, each pinned to its own group.

  Scenario: Each object part on the same operation gets its own group's clone
    When the caller fetches the OpenAPI document
    Then the OpenAPI multipart body schema for POST "/v1/testMultipartTwoParts" part "first" references "MultipartFirstPartDtoCreateGroup"
    And the OpenAPI multipart body schema for POST "/v1/testMultipartTwoParts" part "second" references "MultipartSecondPartDtoUpdateGroup"

  # --- group-cloning audit ---

  # Every rewrite path must leave the document internally consistent. This one assertion
  # catches a clone that was reserved but never registered, a discriminator mapping left
  # pointing at an uncloned subtype, and a component destroyed by a name collision.

  Scenario: Every schema reference in the served document resolves
    When the caller fetches the OpenAPI document
    Then every schema reference in the OpenAPI document resolves to a declared component

  # Each part carries its own pin, so each clone must carry only that part's group.

  Scenario: Each part's clone carries only that part's own group entries
    When the caller fetches the OpenAPI document
    Then the OpenAPI multipart body schema for POST "/v1/testMultipartTwoParts" part "second" has property "label" with x-validations
      | rule     | code  |
      | notBlank | BLANK |
    And the OpenAPI document does not declare schema "MultipartSecondPartDtoCreateGroup"

  # Jakarta treats null as valid for @Size, so a lower bound is not a presence rule.
  # @NotEmpty emits the same rule name and does imply presence, so the two must stay distinct.

  Scenario: Size with a lower bound does not make a nullable property required
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "SizeMinOnlyDto" does not have "value" in its required fields
    And the OpenAPI schema "SizeDto" does not have "value" in its required fields
    And the OpenAPI schema "SizeListDto" does not have "items" in its required fields

  Scenario: NotEmpty still derives required
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "NotEmptyStringDto" has "value" in its required fields
    And the OpenAPI schema "NotEmptyListDto" has "items" in its required fields

  # Presence can be declared outside the constraint set, and filtering a group-scoped
  # constraint out of the default view must not take that declaration with it.

  Scenario: A declared-required field stays required when its only constraint is group-scoped
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "NonNullGroupedDto" has "name" in its required fields
    And the OpenAPI schema "NonNullGroupedDto" has property "name" without an x-validations extension

  # A DTO with no grouped constraints of its own still needs a clone when a descendant has one.

  Scenario: An ungrouped intermediate is cloned so its grouped descendant is reached
    When the caller fetches the OpenAPI document
    Then the OpenAPI document declares schema "TransitiveMiddleDtoCreateGroup"
    And the OpenAPI schema "TransitiveOuterDtoCreateGroup" property "middle" references "TransitiveMiddleDtoCreateGroup"
    And the OpenAPI schema "TransitiveMiddleDtoCreateGroup" property "inner" references "TransitiveInnerDtoCreateGroup"
    And the OpenAPI schema "TransitiveInnerDtoCreateGroup" has "email" in its required fields

  # Cycles must terminate and still point at the clone.

  Scenario: A self-referential DTO points its recursive edge at its own clone
    When the caller fetches the OpenAPI document
    Then the OpenAPI document declares schema "TreeDtoCreateGroup"
    And the OpenAPI schema "TreeDtoCreateGroup" items property "children" references "TreeDtoCreateGroup"

  Scenario: Mutually recursive DTOs are both cloned regardless of which is reached first
    When the caller fetches the OpenAPI document
    Then the OpenAPI document declares schema "NodeADtoCreateGroup"
    And the OpenAPI document declares schema "NodeBDtoCreateGroup"
    And the OpenAPI schema "NodeADtoCreateGroup" property "b" references "NodeBDtoCreateGroup"
    And the OpenAPI schema "NodeBDtoCreateGroup" property "a" references "NodeADtoCreateGroup"

  # A body that is a container has no ref of its own; the element type still needs cloning.

  Scenario: A list-typed body clones its element component
    When the caller fetches the OpenAPI document
    Then the OpenAPI document declares schema "ListElementDtoCreateGroup"
    And the OpenAPI body schema for POST "/v1/testListBodyCreate" items references "ListElementDtoCreateGroup"

  Scenario: A map-typed body clones its value component
    When the caller fetches the OpenAPI document
    Then the OpenAPI document declares schema "MapValueDtoCreateGroup"
    And the OpenAPI body schema for POST "/v1/testMapBodyCreate" additionalProperties references "MapValueDtoCreateGroup"

  # Group identity is the class, not its simple name.

  Scenario: A constraint scoped to a super-group applies to a clone pinned to the sub-group
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "HierarchyDtoStrictGroup" has property "name" with x-validations
      | rule     | code  |
      | notBlank | BLANK |
    And the OpenAPI schema "HierarchyDtoStrictGroup" has "name" in its required fields

  Scenario: Two groups sharing a simple name are not treated as the same group
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "AmbiguousDtoCreateGroup" has property "local" with x-validations
      | rule     | code  |
      | notBlank | BLANK |
    And the OpenAPI schema "AmbiguousDtoCreateGroup" has property "foreign" without an x-validations extension

  # A clone must never overwrite a component a consumer declared.

  Scenario: A derived name that collides with a real component does not overwrite it
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "OrderCreateGroup" has property "unrelated" with x-validations
      | rule     | code  |
      | notBlank | BLANK |

  # A payload bound as a query object is validated per parameter and needs the same treatment.

  Scenario: A group pinned on a query-object parameter clones the referenced component
    When the caller fetches the OpenAPI document
    Then the OpenAPI document declares schema "QueryObjectDtoCreateGroup"
    And the OpenAPI parameter "filter" for POST "/v1/testQueryObjectCreate" references "QueryObjectDtoCreateGroup"

  # A group-scoped parameter constraint must not be published on an unpinned operation.

  Scenario: A group-scoped parameter constraint is not published unconditionally
    When the caller fetches the OpenAPI document
    Then the OpenAPI parameter "code" for POST "/v1/testParamGroupScoped" has no x-validations

  # Constrained maps emit property-count rules.

  Scenario: Size on a map emits property-count entries
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "SizeMapDto" has property "entries" with x-validations
      | rule          | code      |
      | minProperties | TOO_SHORT |
      | maxProperties | TOO_LONG  |

  # Two annotations collapsing to one rule must resolve deterministically.

  Scenario: A body property dedupes constraints that produce the same rule
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "DedupDto" has property "limit" with x-validations
      | rule    | code                     |
      | minimum | MUST_BE_POSITIVE_OR_ZERO |

  # Internal bookkeeping must never reach a consumer.

  Scenario: Component schema entries carry no internal keys
    When the caller fetches the OpenAPI document
    Then the OpenAPI schema "GroupsDtoCreateGroup" x-validations entries carry no internal keys
    And the OpenAPI schema "GroupedRichDtoCreateGroup" x-validations entries carry no internal keys

  # A pinned part must not drag an unpinned sibling into its group.

  Scenario: An unpinned part alongside a pinned one stays on the default group
    When the caller fetches the OpenAPI document
    Then the OpenAPI multipart body schema for POST "/v1/testMultipartMixedPins" part "unpinned" references "MultipartUnpinnedCompanionDto"
    And the OpenAPI schema "MultipartUnpinnedCompanionDto" has property "scoped" without an x-validations extension
    And the OpenAPI document does not declare schema "MultipartUnpinnedCompanionDtoCreateGroup"
