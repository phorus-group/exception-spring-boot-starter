package group.phorus.exception.handlers

/**
 * Represents a single validation error in an [ApiError] response.
 *
 * When bean validation fails (e.g., `@Valid`, `@NotBlank`, `@Email`), the
 * [RestExceptionHandler] collects all constraint violations and converts them
 * to `ValidationError` objects. These are included in the `validationErrors`
 * array of the `ApiError` response.
 *
 * @property obj The name of the object that failed validation (e.g., `"createUserRequest"`)
 * @property field The name of the field that failed (e.g., `"email"`), or `null` for global errors
 * @property rejectedValue The value that was rejected (e.g., `""`, `null`, or the actual invalid value)
 * @property message The validation error message (e.g., `"Cannot be blank"`, `"Invalid email format"`)
 */
data class ValidationError(
    val obj: String,
    val field: String? = null,
    val rejectedValue: Any? = null,
    val message: String? = null,
)
