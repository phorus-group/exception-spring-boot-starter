package group.phorus.exception.handlers

import jakarta.validation.Validation
import jakarta.validation.Validator
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ConstraintToErrorCodeTest {

    private val validator: Validator =
        Validation.buildDefaultValidatorFactory().validator

    private data class NotNullDto(@field:NotNull val v: String? = null)
    private data class NotEmptyDto(@field:NotEmpty val v: String? = null)
    private data class NotBlankDto(@field:NotBlank val v: String? = null)
    private data class NullDto(@field:Null val v: String? = null)
    private data class SizeStringDto(@field:Size(min = 2, max = 5) val v: String = "")
    private data class SizeListDto(@field:Size(min = 2, max = 5) val v: List<Int> = emptyList())
    private data class LengthDto(@field:Length(min = 2, max = 5) val v: String = "")
    private data class MinDto(@field:Min(5) val v: Int = 0)
    private data class DecimalMinDto(@field:DecimalMin("5.0") val v: BigDecimal = BigDecimal.ZERO)
    private data class MaxDto(@field:Max(5) val v: Int = 0)
    private data class DecimalMaxDto(@field:DecimalMax("5.0") val v: BigDecimal = BigDecimal.ZERO)
    private data class RangeDto(@field:Range(min = 5, max = 10) val v: Int = 0)
    private data class PositiveDto(@field:Positive val v: Int = 0)
    private data class PositiveOrZeroDto(@field:PositiveOrZero val v: Int = 0)
    private data class NegativeDto(@field:Negative val v: Int = 0)
    private data class NegativeOrZeroDto(@field:NegativeOrZero val v: Int = 0)
    private data class DigitsDto(@field:Digits(integer = 2, fraction = 1) val v: BigDecimal = BigDecimal.ZERO)
    private data class PatternDto(@field:Pattern(regexp = "[A-Z]+") val v: String = "")
    private data class EmailDto(@field:Email val v: String = "")
    private data class PastDto(@field:Past val v: LocalDate = LocalDate.MIN)
    private data class PastOrPresentDto(@field:PastOrPresent val v: LocalDate = LocalDate.MIN)
    private data class FutureDto(@field:Future val v: LocalDate = LocalDate.MIN)
    private data class FutureOrPresentDto(@field:FutureOrPresent val v: LocalDate = LocalDate.MIN)
    private data class AssertTrueDto(@field:AssertTrue val v: Boolean = false)
    private data class AssertFalseDto(@field:AssertFalse val v: Boolean = false)

    private fun resolveOne(dto: Any): String? {
        val violations = validator.validate(dto)
        assertFalse(violations.isEmpty(), "expected at least one violation")
        return ConstraintToErrorCode.resolve(violations.first())
    }

    @Test fun `NotNull resolves to REQUIRED`() = assertEquals("REQUIRED", resolveOne(NotNullDto()))
    @Test fun `NotEmpty resolves to REQUIRED`() = assertEquals("REQUIRED", resolveOne(NotEmptyDto(v = "")))
    @Test fun `NotBlank resolves to BLANK`() = assertEquals("BLANK", resolveOne(NotBlankDto(v = "")))
    @Test fun `Null resolves to MUST_BE_NULL`() = assertEquals("MUST_BE_NULL", resolveOne(NullDto(v = "x")))

    @Test fun `Size below min resolves to TOO_SHORT`() =
        assertEquals("TOO_SHORT", resolveOne(SizeStringDto(v = "x")))

    @Test fun `Size above max resolves to TOO_LONG`() =
        assertEquals("TOO_LONG", resolveOne(SizeStringDto(v = "abcdef")))

    @Test fun `Size on collection below min resolves to TOO_SHORT`() =
        assertEquals("TOO_SHORT", resolveOne(SizeListDto(v = listOf(1))))

    @Test fun `Length below min resolves to TOO_SHORT`() =
        assertEquals("TOO_SHORT", resolveOne(LengthDto(v = "x")))

    @Test fun `Length above max resolves to TOO_LONG`() =
        assertEquals("TOO_LONG", resolveOne(LengthDto(v = "abcdef")))

    @Test fun `Min resolves to TOO_SMALL`() = assertEquals("TOO_SMALL", resolveOne(MinDto(v = 1)))
    @Test fun `DecimalMin resolves to TOO_SMALL`() = assertEquals("TOO_SMALL", resolveOne(DecimalMinDto(v = BigDecimal.ONE)))
    @Test fun `Max resolves to TOO_LARGE`() = assertEquals("TOO_LARGE", resolveOne(MaxDto(v = 99)))
    @Test fun `DecimalMax resolves to TOO_LARGE`() = assertEquals("TOO_LARGE", resolveOne(DecimalMaxDto(v = BigDecimal("99.0"))))

    @Test fun `Range below min resolves to TOO_SMALL`() =
        assertEquals("TOO_SMALL", resolveOne(RangeDto(v = 1)))

    @Test fun `Range above max resolves to TOO_LARGE`() =
        assertEquals("TOO_LARGE", resolveOne(RangeDto(v = 99)))

    @Test fun `Positive resolves to MUST_BE_POSITIVE`() = assertEquals("MUST_BE_POSITIVE", resolveOne(PositiveDto(v = 0)))
    @Test fun `PositiveOrZero resolves to MUST_BE_POSITIVE_OR_ZERO`() = assertEquals("MUST_BE_POSITIVE_OR_ZERO", resolveOne(PositiveOrZeroDto(v = -1)))
    @Test fun `Negative resolves to MUST_BE_NEGATIVE`() = assertEquals("MUST_BE_NEGATIVE", resolveOne(NegativeDto(v = 0)))
    @Test fun `NegativeOrZero resolves to MUST_BE_NEGATIVE_OR_ZERO`() = assertEquals("MUST_BE_NEGATIVE_OR_ZERO", resolveOne(NegativeOrZeroDto(v = 1)))
    @Test fun `Digits resolves to INVALID_NUMBER_FORMAT`() = assertEquals("INVALID_NUMBER_FORMAT", resolveOne(DigitsDto(v = BigDecimal("123.45"))))
    @Test fun `Pattern resolves to INVALID_FORMAT`() = assertEquals("INVALID_FORMAT", resolveOne(PatternDto(v = "abc")))
    @Test fun `Email resolves to INVALID_EMAIL`() = assertEquals("INVALID_EMAIL", resolveOne(EmailDto(v = "not-an-email")))
    @Test fun `Past resolves to MUST_BE_PAST`() = assertEquals("MUST_BE_PAST", resolveOne(PastDto(v = LocalDate.now().plusYears(1))))
    @Test fun `PastOrPresent resolves to MUST_BE_PAST_OR_PRESENT`() = assertEquals("MUST_BE_PAST_OR_PRESENT", resolveOne(PastOrPresentDto(v = LocalDate.now().plusYears(1))))
    @Test fun `Future resolves to MUST_BE_FUTURE`() = assertEquals("MUST_BE_FUTURE", resolveOne(FutureDto(v = LocalDate.now().minusYears(1))))
    @Test fun `FutureOrPresent resolves to MUST_BE_FUTURE_OR_PRESENT`() = assertEquals("MUST_BE_FUTURE_OR_PRESENT", resolveOne(FutureOrPresentDto(v = LocalDate.now().minusYears(1))))
    @Test fun `AssertTrue resolves to MUST_BE_TRUE`() = assertEquals("MUST_BE_TRUE", resolveOne(AssertTrueDto(v = false)))
    @Test fun `AssertFalse resolves to MUST_BE_FALSE`() = assertEquals("MUST_BE_FALSE", resolveOne(AssertFalseDto(v = true)))

    @Test
    fun `publicAttributes strips groups payload and message`() {
        val violations = validator.validate(SizeStringDto(v = "x"))
        val attrs = ConstraintToErrorCode.publicAttributes(violations.first().constraintDescriptor)
        assertNotNull(attrs)
        assertTrue(attrs!!.containsKey("min"), "min should be present")
        assertTrue(attrs.containsKey("max"), "max should be present")
        assertFalse(attrs.containsKey("groups"), "groups should be stripped")
        assertFalse(attrs.containsKey("payload"), "payload should be stripped")
        assertFalse(attrs.containsKey("message"), "message should be stripped")
    }

    @Test
    fun `publicAttributes returns null when no public attributes remain`() {
        // @NotBlank carries only message/groups/payload, no public attributes.
        val violations = validator.validate(NotBlankDto(v = ""))
        val attrs = ConstraintToErrorCode.publicAttributes(violations.first().constraintDescriptor)
        // attrs is either null or an empty map filtered down; the contract is null
        assertEquals(null, attrs)
    }
}
