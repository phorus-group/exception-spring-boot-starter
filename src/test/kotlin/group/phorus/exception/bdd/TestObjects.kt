package group.phorus.exception.bdd

import group.phorus.exception.bdd.other.CreateGroup as ForeignCreateGroup
import io.swagger.v3.oas.annotations.media.Schema as SwaggerSchema
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertFalse
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Negative
import jakarta.validation.constraints.NegativeOrZero
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Null
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.Length
import org.hibernate.validator.constraints.Range
import java.math.BigDecimal
import java.time.LocalDate

data class TestObject(
    @field:NotBlank(message = "Cannot be blank")
    val testVar: String? = null,

    @field:NotNull(message = "Cannot be null")
    val testInt: Int? = null,

    @field:Valid
    @field:NotNull(message = "Cannot be null")
    val subObject: TestSubObject? = null,

    @field:Valid
    @field:NotEmpty(message = "Cannot be empty")
    val subObjectList: List<TestSubObject>? = null,
)

data class TestSubObject(
    @field:NotBlank(message = "Cannot be blank")
    val testVar: String,
)

/**
 * Single-field DTOs that exercise one Jakarta constraint each. The validations feature
 * file POSTs invalid payloads and asserts the per-field `code` and `metadata`.
 */
data class NotNullDto(
    @field:NotNull
    val value: String? = null,
)

data class NotEmptyStringDto(
    @field:NotEmpty
    val value: String? = null,
)

data class NotBlankDto(
    @field:NotBlank
    val value: String? = null,
)

data class NullDto(
    @field:Null
    val value: String? = null,
)

data class SizeDto(
    @field:Size(min = 2, max = 10)
    val value: String? = null,
)

data class LengthDto(
    @field:Length(min = 2, max = 10)
    val value: String? = null,
)

data class MinDto(
    @field:Min(5)
    val value: Int? = null,
)

data class DecimalMinDto(
    @field:DecimalMin("5.0")
    val value: BigDecimal? = null,
)

data class MaxDto(
    @field:Max(10)
    val value: Int? = null,
)

data class DecimalMaxDto(
    @field:DecimalMax("10.0")
    val value: BigDecimal? = null,
)

data class RangeDto(
    @field:Range(min = 5, max = 10)
    val value: Int? = null,
)

data class PositiveDto(
    @field:Positive
    val value: Int? = null,
)

data class PositiveOrZeroDto(
    @field:PositiveOrZero
    val value: Int? = null,
)

data class NegativeDto(
    @field:Negative
    val value: Int? = null,
)

data class NegativeOrZeroDto(
    @field:NegativeOrZero
    val value: Int? = null,
)

data class DigitsDto(
    @field:Digits(integer = 3, fraction = 2)
    val value: BigDecimal? = null,
)

data class PatternDto(
    @field:Pattern(regexp = "[A-Z]+")
    val value: String? = null,
)

data class EmailDto(
    @field:Email
    val value: String? = null,
)

data class PastDto(
    @field:Past
    val value: LocalDate? = null,
)

data class PastOrPresentDto(
    @field:PastOrPresent
    val value: LocalDate? = null,
)

data class FutureDto(
    @field:Future
    val value: LocalDate? = null,
)

data class FutureOrPresentDto(
    @field:FutureOrPresent
    val value: LocalDate? = null,
)

data class AssertTrueDto(
    @field:AssertTrue
    val value: Boolean? = null,
)

data class AssertFalseDto(
    @field:AssertFalse
    val value: Boolean? = null,
)

/**
 * Kitchen-sink DTO exercising multiple constraints on a single field. The openapi
 * feature file fetches `/v3/api-docs` and asserts the produced `x-validations` array
 * carries one entry per recognised constraint, in declaration order.
 */
data class KitchenSinkDto(
    @field:NotBlank
    @field:Size(min = 2, max = 10)
    @field:Pattern(regexp = "[A-Z]+")
    val name: String? = null,

    val description: String? = null,
)

/**
 * DTOs exercising the type-aware boundary rules and one-side gating of `@Size` and
 * `@Range`, used by the openapi feature file only.
 */
data class SizeListDto(
    @field:Size(min = 1, max = 5)
    val items: List<String>? = null,
)

data class NotEmptyListDto(
    @field:NotEmpty
    val items: List<String>? = null,
)

data class SizeMaxOnlyDto(
    @field:Size(max = 10)
    val value: String? = null,
)

data class RangeMaxOnlyDto(
    @field:Range(max = 100)
    val value: Int? = null,
)

data class RangeMinOnlyDto(
    @field:Range(min = 5)
    val value: Int? = null,
)

/**
 * Validation group markers used by [GroupsDto]. Mirror the
 * production-side `Create` / `Update` groups so the openapi customizer
 * has to scope `x-validations` and `required` per active group.
 */
interface CreateGroup
interface UpdateGroup

/**
 * DTO with constraints scoped to validation groups. `@NotBlank` is
 * active on the `Create` group only; `@Size` is active on both. Two
 * controller endpoints reference this same DTO but with different
 * `@Validated` groups, so the customizer must emit two distinct schema
 * components (one per active group) with the correct `x-validations`
 * and `required` for each.
 */
data class GroupsDto(
    @field:NotBlank(groups = [CreateGroup::class])
    @field:Size(max = 100, groups = [CreateGroup::class, UpdateGroup::class])
    val name: String? = null,
)

/**
 * DTO matching the production "no groups" pattern: the same DTO is
 * referenced by two endpoints that use only `@Valid` (no `@Validated`),
 * so all constraints apply on both operations. Used to guard against
 * the group-aware customizer mis-cloning ungrouped DTOs or stripping
 * their constraints.
 */
data class UngroupedDto(
    @field:NotBlank
    @field:Size(max = 100)
    val name: String? = null,
)

/**
 * Outer DTO with a cascading nested field. Both the outer field and the
 * nested DTO carry constraints scoped to validation groups. JSR 380 §5.4.5
 * dictates the outer group propagates through `@Valid` cascading, so the
 * customizer must clone the inner DTO per active group too, not only the
 * outer one.
 */
data class OuterGroupsDto(
    @field:NotBlank(groups = [CreateGroup::class])
    @field:Size(max = 100, groups = [CreateGroup::class, UpdateGroup::class])
    val name: String? = null,

    @field:Valid
    val inner: InnerGroupsDto? = null,
)

data class InnerGroupsDto(
    @field:NotBlank(groups = [CreateGroup::class])
    val email: String? = null,

    @field:NotBlank(groups = [UpdateGroup::class])
    val displayName: String? = null,
)

/**
 * Three-level DTO chain used by the multi-level cascade tests. Each level carries a
 * group-scoped constraint and a `@field:Valid` cascade into the next; the customizer
 * must clone all three components per active group.
 */
data class Level1Dto(
    @field:NotBlank(groups = [CreateGroup::class])
    val name: String? = null,

    @field:Valid
    val level2: Level2Dto? = null,
)

data class Level2Dto(
    @field:NotBlank(groups = [CreateGroup::class])
    val name: String? = null,

    @field:Valid
    val level3: Level3Dto? = null,
)

data class Level3Dto(
    @field:NotBlank(groups = [CreateGroup::class])
    val name: String? = null,
)

/**
 * Outer DTO whose nested DTO carries no group-scoped constraints. The outer is cloned
 * per group, but its `plain` property must keep the original `$ref` since the inner
 * has nothing to filter or scope.
 */
data class OuterWithPlainInnerDto(
    @field:NotBlank(groups = [CreateGroup::class])
    val name: String? = null,

    @field:Valid
    val plain: PlainInnerDto? = null,
)

data class PlainInnerDto(
    @field:NotBlank
    val value: String? = null,
)

/**
 * DTO whose only constraint is group-scoped and that is consumed by `@Validated(Group)`
 * endpoints only, never by `@Valid`. Used to verify the original component is pruned from
 * `components.schemas` when no consumer references it directly.
 */
data class OnlyGroupsOrphanDto(
    @field:NotBlank(groups = [CreateGroup::class])
    val value: String? = null,
)

/**
 * DTO with one field per Jakarta + Hibernate annotation that produces a JSON Schema validator.
 * Every field's constraint is scoped to `CreateGroup`. Two endpoints reference this DTO, one
 * pinned to `Create` and one using `@Valid`, so the per-group clone and the default-view
 * original can be asserted side by side for every annotation family.
 */
data class GroupedValidatorsDto(
    @field:NotEmpty(groups = [CreateGroup::class])
    val notEmptyString: String? = null,

    @field:NotEmpty(groups = [CreateGroup::class])
    val notEmptyList: List<String>? = null,

    @field:Size(min = 2, max = 50, groups = [CreateGroup::class])
    val sizeString: String? = null,

    @field:Size(min = 2, max = 50, groups = [CreateGroup::class])
    val sizeList: List<String>? = null,

    @field:org.hibernate.validator.constraints.Length(min = 2, max = 50, groups = [CreateGroup::class])
    val lengthString: String? = null,

    @field:jakarta.validation.constraints.Min(value = 5, groups = [CreateGroup::class])
    val minInt: Int? = null,

    @field:jakarta.validation.constraints.Max(value = 50, groups = [CreateGroup::class])
    val maxInt: Int? = null,

    @field:jakarta.validation.constraints.DecimalMin(value = "5.5", groups = [CreateGroup::class])
    val decimalMin: java.math.BigDecimal? = null,

    @field:jakarta.validation.constraints.DecimalMax(value = "50.5", groups = [CreateGroup::class])
    val decimalMax: java.math.BigDecimal? = null,

    @field:org.hibernate.validator.constraints.Range(min = 5, max = 50, groups = [CreateGroup::class])
    val rangeInt: Int? = null,

    @field:jakarta.validation.constraints.Positive(groups = [CreateGroup::class])
    val positiveInt: Int? = null,

    @field:jakarta.validation.constraints.PositiveOrZero(groups = [CreateGroup::class])
    val positiveOrZeroInt: Int? = null,

    @field:jakarta.validation.constraints.Negative(groups = [CreateGroup::class])
    val negativeInt: Int? = null,

    @field:jakarta.validation.constraints.NegativeOrZero(groups = [CreateGroup::class])
    val negativeOrZeroInt: Int? = null,

    @field:jakarta.validation.constraints.Pattern(regexp = "[A-Z]+", groups = [CreateGroup::class])
    val patternString: String? = null,

    @field:jakarta.validation.constraints.Email(groups = [CreateGroup::class])
    val emailString: String? = null,
)

/**
 * DTO with rich `@Schema`-driven metadata on group-scoped fields. The clone walker must
 * preserve `format` (uuid / int32), `description`, `title`, `example`, `enum`, and
 * `default` so the served per-group component matches the source DTO's contract.
 */
enum class StatusEnum { ACTIVE, INACTIVE }

data class GroupedRichDto(
    @field:jakarta.validation.constraints.NotNull(groups = [CreateGroup::class])
    @field:SwaggerSchema(description = "Unique user identifier", title = "User id")
    val id: java.util.UUID? = null,

    @field:jakarta.validation.constraints.NotBlank(groups = [CreateGroup::class])
    @field:SwaggerSchema(description = "Display name", example = "Alice", title = "Name")
    val name: String? = null,

    @field:jakarta.validation.constraints.Min(value = 5, groups = [CreateGroup::class])
    @field:SwaggerSchema(description = "Number of items")
    val count: Int? = null,

    @field:jakarta.validation.constraints.NotNull(groups = [CreateGroup::class])
    @field:SwaggerSchema(description = "Current status", defaultValue = "ACTIVE")
    val status: StatusEnum? = null,
)

/**
 * DTO with a field carrying BOTH an ungrouped constraint and a group-scoped one. The
 * ungrouped entry (default group) must survive every per-group filter; the group-scoped
 * one is kept only when its group is active.
 */
data class MixedConstraintsDto(
    @field:NotBlank
    @field:Pattern(regexp = "[A-Z]+", groups = [CreateGroup::class])
    val name: String? = null,
)

/**
 * DTO whose cascading `@Valid` is on a List element. Per JSR 380 TYPE_USE container
 * element constraints, the outer group propagates into each element's cascade. The
 * customizer must walk `items.$ref` (not just `$ref` on the property itself) to find
 * the inner component and clone it.
 */
data class CollectionGroupsDto(
    @field:Valid
    val items: List<InnerGroupsDto>? = null,
)

/**
 * DTO used only as a response body, never as a request body. The orphan-pruning walk
 * must reach it through `operation.responses[*].content[*].schema` so the response's
 * `$ref` keeps resolving to a real component in `components.schemas`.
 */
data class ResponseOnlyDto(
    @field:NotBlank
    val name: String? = null,
)

/**
 * DTO consumed only by a multipart endpoint, pinned to `CreateGroup` on a `@RequestPart`
 * rather than a `@RequestBody`. Both annotations pin the same group and Spring enforces
 * both at runtime, so the per-group clone must be derived for either.
 */
data class MultipartPartDto(
    @field:NotBlank(groups = [CreateGroup::class])
    val name: String? = null,
)

/**
 * DTO reachable from a multipart endpoint pinned to `CreateGroup` and a JSON endpoint pinned
 * to `UpdateGroup`, the production shape where one DTO serves create and update. Both clones
 * must be derived, each carrying only its own group's constraints.
 */
data class MultipartSharedDto(
    @field:NotBlank(groups = [CreateGroup::class])
    val name: String? = null,

    @field:Size(max = 50, groups = [UpdateGroup::class])
    val description: String? = null,
)

/**
 * DTO consumed by a multipart part carrying `@Valid` and no group pin. No clone should be
 * derived; the original component keeps its constraints.
 */
data class MultipartUngroupedDto(
    @field:NotBlank
    val value: String? = null,
)

/**
 * DTO consumed by a multipart endpoint whose group pin lives on the method rather than on the
 * part parameter, mirroring the existing method-level `@RequestBody` case.
 */
data class MultipartMethodLevelDto(
    @field:NotBlank(groups = [CreateGroup::class])
    val value: String? = null,
)

/**
 * Outer DTO reached from a multipart part, cascading into [MultipartInnerDto]. The customizer
 * must clone the nested component per active group the same way it does for a request body.
 */
data class MultipartOuterDto(
    @field:NotBlank(groups = [CreateGroup::class])
    val name: String? = null,

    @field:Valid
    val inner: MultipartInnerDto? = null,
)

data class MultipartInnerDto(
    @field:NotBlank(groups = [CreateGroup::class])
    val email: String? = null,

    @field:NotBlank(groups = [UpdateGroup::class])
    val displayName: String? = null,
)

/**
 * DTO whose constraints live in two groups, consumed by a part pinned to both. One combined
 * clone must carry every active group's entries.
 */
data class MultipartMultiGroupDto(
    @field:NotBlank(groups = [CreateGroup::class])
    @field:Size(max = 100, groups = [UpdateGroup::class])
    val name: String? = null,
)

/**
 * DTO reached only through a grouped multipart part, never through `@Valid`. Once the clone
 * exists the original has no consumer and must be pruned, as it is for a grouped body.
 */
data class MultipartOrphanDto(
    @field:NotBlank(groups = [CreateGroup::class])
    val value: String? = null,
)

/** First object part of a two-part endpoint, pinned to `CreateGroup`. */
data class MultipartFirstPartDto(
    @field:NotBlank(groups = [CreateGroup::class])
    val name: String? = null,
)

/** Second object part of the same endpoint, pinned to a different group. */
data class MultipartSecondPartDto(
    @field:NotBlank(groups = [UpdateGroup::class])
    val label: String? = null,
)


// ---------------------------------------------------------------------------
// Fixtures for the group-cloning audit. Each block names the behaviour it pins.
// ---------------------------------------------------------------------------

/** `interface StrictGroup : BasicGroup` — JSR 380 runs BasicGroup constraints for StrictGroup. */
interface BasicGroup
interface StrictGroup : BasicGroup

/** Constraint scoped to the super-group; a clone pinned to the sub-group must keep it. */
data class HierarchyDto(
    @field:NotBlank(groups = [BasicGroup::class])
    val name: String? = null,
)

/** Nullable field whose only constraint is a lower bound: `null` satisfies `@Size`. */
data class SizeMinOnlyDto(
    @field:Size(min = 2)
    val value: String? = null,
)

/**
 * Field declared required outside the constraint set, carrying one group-scoped constraint.
 * Filtering to the default view drops the constraint but the declared presence stands.
 */
data class NonNullGroupedDto(
    @field:Size(max = 10, groups = [CreateGroup::class])
    @field:SwaggerSchema(requiredMode = SwaggerSchema.RequiredMode.REQUIRED)
    val name: String,
)

/** Grouped outer, ungrouped middle, grouped inner: the middle must still be cloned. */
data class TransitiveOuterDto(
    @field:NotBlank(groups = [CreateGroup::class])
    val name: String? = null,

    @field:Valid
    val middle: TransitiveMiddleDto? = null,
)

data class TransitiveMiddleDto(
    @field:Valid
    val inner: TransitiveInnerDto? = null,
)

data class TransitiveInnerDto(
    @field:NotBlank(groups = [CreateGroup::class])
    val email: String? = null,
)

/** Self-referential: the clone's recursive edge must point at the clone. */
data class TreeDto(
    @field:NotBlank(groups = [CreateGroup::class])
    val name: String? = null,

    @field:Valid
    val children: List<TreeDto>? = null,
)

/** Mutually recursive, with the grouped constraint only on the leaf reached from A. */
data class NodeADto(
    @field:Valid
    val b: NodeBDto? = null,

    @field:Valid
    val leaf: NodeLeafDto? = null,
)

data class NodeBDto(
    @field:Valid
    val a: NodeADto? = null,
)

data class NodeLeafDto(
    @field:NotBlank(groups = [CreateGroup::class])
    val value: String? = null,
)

/** Element type of a list-typed body. */
data class ListElementDto(
    @field:NotBlank(groups = [CreateGroup::class])
    val name: String? = null,
)

/** Value type of a map-typed body. */
data class MapValueDto(
    @field:NotBlank(groups = [CreateGroup::class])
    val name: String? = null,
)

/** Constrained map, for the minProperties / maxProperties rules. */
data class SizeMapDto(
    @field:Size(min = 1, max = 5)
    val entries: Map<String, String>? = null,
)

/** Two annotations that collapse to the same rule name, to pin de-duplication order. */
data class DedupDto(
    @field:PositiveOrZero
    @field:Min(0)
    val limit: Int? = null,
)

/** Bound to a query object, so the payload arrives as a parameter rather than a body. */
data class QueryObjectDto(
    @field:NotBlank(groups = [CreateGroup::class])
    val term: String? = null,
)

/** Hand-written component whose name is exactly what a clone of [Order] would be called. */
data class OrderCreateGroup(
    @field:NotBlank
    val unrelated: String? = null,
)

data class Order(
    @field:NotBlank(groups = [CreateGroup::class])
    val reference: String? = null,
)

/**
 * One field pinned to this package's `CreateGroup`, one to the same-named group in another
 * package. Only the first belongs in a clone pinned to this package's group.
 */
data class AmbiguousDto(
    @field:NotBlank(groups = [CreateGroup::class])
    val local: String? = null,

    @field:NotBlank(groups = [ForeignCreateGroup::class])
    val foreign: String? = null,
)

/** Unpinned companion to a pinned part on the same operation: it runs the Default group. */
data class MultipartUnpinnedCompanionDto(
    @field:NotBlank(groups = [CreateGroup::class])
    val scoped: String? = null,
)

