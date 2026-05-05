package group.phorus.exception.handlers

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import group.phorus.exception.core.ReservedErrorCodes
import jakarta.validation.ConstraintViolation
import jakarta.validation.metadata.ConstraintDescriptor
import org.hibernate.validator.internal.engine.path.PathImpl
import org.springframework.http.HttpStatus
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import java.time.LocalDateTime

/**
 * Standardized error response returned by [RestExceptionHandler] and [WebfluxExceptionHandler].
 *
 * The structure follows [RFC 9457 (Problem Details for HTTP APIs)](https://www.rfc-editor.org/rfc/rfc9457.html)
 * naming conventions for the core fields (`status`, `title`, `detail`), extended with
 * application-specific fields (`code`, `timestamp`, `validationErrors`).
 *
 * The `code` property is always non-null. When the exception that produced the response
 * does not provide an explicit code, the handler resolves a reserved fallback from
 * [ReservedErrorCodes] (for example `"BAD_REQUEST"` for status 400, `"VALIDATION_FAILED"`
 * for `@Valid` failures, `"INTERNAL_SERVER_ERROR"` for uncaught exceptions).
 *
 * Example JSON response:
 * ```json
 * {
 *   "timestamp": "06-03-2026 10:30:00",
 *   "status": 404,
 *   "title": "Not Found",
 *   "detail": "User with id 550e8400-e29b-41d4-a716-446655440000 not found",
 *   "code": "NOT_FOUND"
 * }
 * ```
 *
 * With an explicit application code:
 * ```json
 * {
 *   "timestamp": "06-03-2026 10:30:00",
 *   "status": 400,
 *   "title": "Bad Request",
 *   "detail": "Email format is invalid",
 *   "code": "VALIDATION_EMAIL"
 * }
 * ```
 *
 * For validation errors, the response includes a `validationErrors` array. Each entry
 * carries the reserved code from [ReservedErrorCodes] for the failing Jakarta constraint
 * (`BLANK` for `@NotBlank`, `TOO_SHORT` or `TOO_LONG` for `@Size`, etc.) and the public
 * attributes of that constraint (`min`, `max`, `regexp`):
 * ```json
 * {
 *   "timestamp": "06-03-2026 10:30:00",
 *   "status": 400,
 *   "title": "Bad Request",
 *   "detail": "Validation error",
 *   "code": "VALIDATION_FAILED",
 *   "validationErrors": [
 *     {
 *       "obj": "createUserRequest",
 *       "field": "email",
 *       "code": "BLANK",
 *       "rejectedValue": null,
 *       "message": "Cannot be blank"
 *     }
 *   ]
 * }
 * ```
 *
 * @property status HTTP status code as an integer (e.g. `400`, `404`, `500`).
 * @property title Short label for the HTTP status (e.g. `"Bad Request"`, `"Not Found"`).
 * @property detail Human-readable explanation of this specific error occurrence.
 * @property code Application-specific error code for programmatic handling.
 * @property source Identifier of the service that produced this error (e.g. `"user-service"`). Omitted from JSON when null.
 * @property metadata Extra context about the error as key-value pairs. Omitted from JSON when null.
 */
data class ApiError(
    val status: Int,
    val title: String,
    val detail: String? = null,
    val code: String,
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val source: String? = null,
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val metadata: Map<String, Any?>? = null,
) {

    /** Timestamp when the error occurred, formatted as "dd-MM-yyyy hh:mm:ss" */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    val timestamp: LocalDateTime = LocalDateTime.now()

    /** List of validation errors. Only present when validation fails (e.g., `@Valid` annotation). */
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    var validationErrors: MutableList<ValidationError>? = null

    private fun addValidationError(
        obj: String,
        message: String?,
        field: String? = null,
        rejectedValue: Any? = null,
        code: String? = null,
        metadata: Map<String, Any?>? = null,
    ) {
        if (validationErrors == null) validationErrors = mutableListOf()
        validationErrors!!.add(ValidationError(
            obj = obj,
            field = field,
            code = code,
            rejectedValue = rejectedValue,
            message = message,
            metadata = metadata,
        ))
    }

    /**
     * Adds field-level validation errors from Spring's `FieldError` objects.
     *
     * Called by [RestExceptionHandler] when handling `WebExchangeBindException`. The
     * [codeResolver] callback returns the reserved code for the failing constraint and
     * the [metadataResolver] callback returns the public attributes of the constraint
     * (`min`, `max`, `regexp`, etc.). Both default to returning `null` so callers that
     * do not derive per-field metadata still get a working overload.
     */
    fun addValidationErrors(
        fieldErrors: List<FieldError>,
        codeResolver: (FieldError) -> String? = { null },
        metadataResolver: (FieldError) -> Map<String, Any?>? = { null },
    ) =
        fieldErrors.forEach {
            addValidationError(
                obj = it.objectName,
                message = it.defaultMessage,
                field = it.field,
                rejectedValue = it.rejectedValue,
                code = codeResolver(it),
                metadata = metadataResolver(it),
            )
        }

    /**
     * Adds global (non-field) validation errors from Spring's `ObjectError` objects.
     *
     * Called by [RestExceptionHandler] when handling `WebExchangeBindException`.
     */
    fun addValidationError(globalErrors: List<ObjectError>) =
        globalErrors.forEach {
            addValidationError(
                obj = it.objectName,
                message = it.defaultMessage,
            )
        }

    /**
     * Adds constraint violations from Jakarta Bean Validation.
     *
     * Called by [RestExceptionHandler] when handling `ConstraintViolationException`,
     * which occurs when `@Validated` is used on controllers for method-level validation.
     * The [codeResolver] callback returns the reserved code for the failing constraint
     * and the [metadataResolver] callback returns the public attributes of the
     * [ConstraintDescriptor]. Both default to returning `null`.
     */
    fun addValidationErrors(
        constraintViolations: Set<ConstraintViolation<*>>,
        codeResolver: (ConstraintViolation<*>) -> String? = { null },
        metadataResolver: (ConstraintViolation<*>) -> Map<String, Any?>? = { null },
    ) =
        constraintViolations.forEach {
            addValidationError(
                obj = it.rootBeanClass.simpleName,
                message = it.message,
                field = (it.propertyPath as PathImpl).leafNode.asString(),
                rejectedValue = it.invalidValue,
                code = codeResolver(it),
                metadata = metadataResolver(it),
            )
        }

    companion object {
        fun of(
            httpStatus: HttpStatus,
            detail: String? = null,
            code: String,
            source: String? = null,
            metadata: Map<String, Any?>? = null,
        ): ApiError = ApiError(
            status = httpStatus.value(),
            title = httpStatus.reasonPhrase,
            detail = detail,
            code = code,
            source = source,
            metadata = metadata,
        )
    }
}
