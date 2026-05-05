package group.phorus.exception.bdd

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
