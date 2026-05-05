package group.phorus.exception.handlers

import com.fasterxml.jackson.annotation.JsonInclude
import group.phorus.exception.core.ReservedErrorCodes

/**
 * Represents a single validation error in an [ApiError] response.
 *
 * When bean validation fails (e.g. `@Valid`, `@NotBlank`, `@Email`), the
 * [RestExceptionHandler] collects all constraint violations and converts them
 * to `ValidationError` objects. These are included in the `validationErrors`
 * array of the [ApiError] response.
 *
 * The optional [code] property is the reserved [ReservedErrorCodes] constant for the
 * Jakarta constraint annotation that triggered the violation (`BLANK` for `@NotBlank`,
 * `TOO_LONG` for `@Size(max = ...)` exceeded, etc.). When the constraint annotation type
 * is not in the mapping, [code] is `null` and Jackson omits the property.
 *
 * The optional [metadata] property carries the public attributes of the failing
 * constraint (`min`, `max`, `regexp`, etc.). Bean Validation infrastructure entries
 * (`groups`, `payload`, default `message` template) are stripped. Useful for clients
 * that interpolate constraint parameters into a localized error message.
 *
 * @property obj The name of the object that failed validation (e.g. `"createUserRequest"`).
 * @property field The name of the field that failed (e.g. `"email"`), or `null` for global errors.
 * @property code Reserved error code for the failing constraint, or `null` when the constraint type is unmapped.
 * @property rejectedValue The value that was rejected (e.g. `""`, `null`, or the actual invalid value).
 * @property message The validation error message (e.g. `"Cannot be blank"`, `"Invalid email format"`).
 * @property metadata Public attributes of the failing constraint, or `null` when no attributes remain after filtering.
 */
data class ValidationError(
    val obj: String,
    val field: String? = null,
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val code: String? = null,
    val rejectedValue: Any? = null,
    val message: String? = null,
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val metadata: Map<String, Any?>? = null,
)
