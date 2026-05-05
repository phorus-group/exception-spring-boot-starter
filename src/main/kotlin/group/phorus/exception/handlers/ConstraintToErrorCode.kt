package group.phorus.exception.handlers

import group.phorus.exception.core.ReservedErrorCodes
import jakarta.validation.ConstraintViolation
import jakarta.validation.metadata.ConstraintDescriptor

/**
 * Maps a Jakarta Bean Validation [ConstraintViolation] to the matching reserved error
 * code from [ReservedErrorCodes].
 *
 * Constraint annotations are matched by canonical name, so the implementation does not
 * require the corresponding annotation classes to be on the runtime classpath. Hibernate
 * Validator extensions (`@Length`, `@Range`) are matched the same way and resolved on a
 * best-effort basis.
 *
 * For directional constraints (`@Size`, `@Length`, `@Range`) the value side is computed
 * by reading the violation's `invalidValue` and comparing its size or numeric magnitude
 * against the `min` and `max` attributes from the [ConstraintDescriptor]. When both
 * sides are theoretically violated by a single value (which Bean Validation does not
 * normally produce), the implementation falls back to the upper-bound code (`TOO_LONG`
 * or `TOO_LARGE`).
 *
 * Returns `null` for any constraint annotation type the mapper does not recognise. The
 * starter omits the per-field `code` in that case.
 */
internal object ConstraintToErrorCode {

    private const val JAKARTA = "jakarta.validation.constraints"
    private const val HIBERNATE = "org.hibernate.validator.constraints"

    /**
     * Returns the reserved error code for the given violation, or `null` when the
     * constraint annotation type is not in the mapping table.
     */
    fun resolve(violation: ConstraintViolation<*>): String? {
        val descriptor = violation.constraintDescriptor
        val annotationType = descriptor.annotation.annotationClass.java
        return when (annotationType.canonicalName) {
            "$JAKARTA.NotNull"  -> ReservedErrorCodes.REQUIRED
            "$JAKARTA.NotEmpty" -> ReservedErrorCodes.REQUIRED
            "$JAKARTA.NotBlank" -> ReservedErrorCodes.BLANK
            "$JAKARTA.Null"     -> ReservedErrorCodes.MUST_BE_NULL
            "$JAKARTA.Size"     -> resolveSize(violation, descriptor)
            "$HIBERNATE.Length" -> resolveSize(violation, descriptor)
            "$JAKARTA.Min"           -> ReservedErrorCodes.TOO_SMALL
            "$JAKARTA.DecimalMin"    -> ReservedErrorCodes.TOO_SMALL
            "$JAKARTA.Max"           -> ReservedErrorCodes.TOO_LARGE
            "$JAKARTA.DecimalMax"    -> ReservedErrorCodes.TOO_LARGE
            "$HIBERNATE.Range"       -> resolveRange(violation, descriptor)
            "$JAKARTA.Positive"          -> ReservedErrorCodes.MUST_BE_POSITIVE
            "$JAKARTA.PositiveOrZero"    -> ReservedErrorCodes.MUST_BE_POSITIVE_OR_ZERO
            "$JAKARTA.Negative"          -> ReservedErrorCodes.MUST_BE_NEGATIVE
            "$JAKARTA.NegativeOrZero"    -> ReservedErrorCodes.MUST_BE_NEGATIVE_OR_ZERO
            "$JAKARTA.Digits"            -> ReservedErrorCodes.INVALID_NUMBER_FORMAT
            "$JAKARTA.Pattern"           -> ReservedErrorCodes.INVALID_FORMAT
            "$JAKARTA.Email"             -> ReservedErrorCodes.INVALID_EMAIL
            "$JAKARTA.Past"              -> ReservedErrorCodes.MUST_BE_PAST
            "$JAKARTA.PastOrPresent"     -> ReservedErrorCodes.MUST_BE_PAST_OR_PRESENT
            "$JAKARTA.Future"            -> ReservedErrorCodes.MUST_BE_FUTURE
            "$JAKARTA.FutureOrPresent"   -> ReservedErrorCodes.MUST_BE_FUTURE_OR_PRESENT
            "$JAKARTA.AssertTrue"        -> ReservedErrorCodes.MUST_BE_TRUE
            "$JAKARTA.AssertFalse"       -> ReservedErrorCodes.MUST_BE_FALSE
            else -> null
        }
    }

    /**
     * Returns the public attributes of the failing constraint that are useful to the
     * caller (`min`, `max`, `regexp`, etc.).
     *
     * Strips Bean Validation infrastructure entries (`groups`, `payload`) and the default
     * `message` template. Returns `null` when no public attribute remains.
     */
    fun publicAttributes(descriptor: ConstraintDescriptor<*>): Map<String, Any?>? {
        val attrs = descriptor.attributes
            .filterKeys { it != "groups" && it != "payload" && it != "message" }
        return attrs.takeIf { it.isNotEmpty() }
    }

    private fun resolveSize(violation: ConstraintViolation<*>, descriptor: ConstraintDescriptor<*>): String {
        val min = (descriptor.attributes["min"] as? Number)?.toLong() ?: 0L
        val max = (descriptor.attributes["max"] as? Number)?.toLong() ?: Long.MAX_VALUE
        val actual = sizeOf(violation.invalidValue)
        return when {
            actual == null -> ReservedErrorCodes.TOO_LONG
            actual < min -> ReservedErrorCodes.TOO_SHORT
            actual > max -> ReservedErrorCodes.TOO_LONG
            else -> ReservedErrorCodes.TOO_LONG
        }
    }

    private fun resolveRange(violation: ConstraintViolation<*>, descriptor: ConstraintDescriptor<*>): String {
        val min = (descriptor.attributes["min"] as? Number)?.toLong()
        val max = (descriptor.attributes["max"] as? Number)?.toLong()
        val actual = (violation.invalidValue as? Number)?.toLong()
        return when {
            actual == null -> ReservedErrorCodes.TOO_LARGE
            min != null && actual < min -> ReservedErrorCodes.TOO_SMALL
            max != null && actual > max -> ReservedErrorCodes.TOO_LARGE
            else -> ReservedErrorCodes.TOO_LARGE
        }
    }

    private fun sizeOf(value: Any?): Long? = when (value) {
        null -> null
        is CharSequence -> value.length.toLong()
        is Collection<*> -> value.size.toLong()
        is Map<*, *> -> value.size.toLong()
        is Array<*> -> value.size.toLong()
        is BooleanArray -> value.size.toLong()
        is ByteArray -> value.size.toLong()
        is CharArray -> value.size.toLong()
        is ShortArray -> value.size.toLong()
        is IntArray -> value.size.toLong()
        is LongArray -> value.size.toLong()
        is FloatArray -> value.size.toLong()
        is DoubleArray -> value.size.toLong()
        else -> null
    }
}
