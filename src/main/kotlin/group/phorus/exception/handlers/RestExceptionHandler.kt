package group.phorus.exception.handlers

import group.phorus.exception.core.BaseException
import group.phorus.exception.config.MetricsRecorder
import group.phorus.exception.config.SourceResolver
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebInputException
import java.util.concurrent.TimeoutException

private val PROBLEM_JSON = MediaType.parseMediaType("application/problem+json")

/**
 * Controller exception handler that catches exceptions thrown inside `@RestController` methods
 * and converts them to standardized [ApiError] JSON responses.
 *
 * This handler is registered as `@RestControllerAdvice` and catches exceptions from:
 * - Business logic ([BaseException] subclasses like `NotFound`, `BadRequest`, etc.)
 * - Bean validation failures (`@Valid`, `@Validated` annotations)
 * - Type conversion errors (invalid path variables, query params)
 * - Database constraint violations
 * - Spring framework exceptions
 *
 * All caught exceptions are logged at **debug level** (configurable via `application.yml`)
 * and optionally recorded as metrics (see [MetricsRecorder]).
 *
 * **Logging configuration:**
 * ```yaml
 * logging:
 *   level:
 *     group.phorus.exception: DEBUG  # Shows all exception logs
 * ```
 *
 * **Note:** This handler does **not** catch exceptions thrown in WebFilters: those are
 * handled by [WebfluxExceptionHandler].
 *
 * @see WebfluxExceptionHandler for filter-level exception handling
 * @see MetricsRecorder for optional metrics recording
 */
@RestControllerAdvice
class RestExceptionHandler(
    metricsProvider: ObjectProvider<MetricsRecorder>,
    private val sourceResolver: SourceResolver,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val metrics = metricsProvider.getIfAvailable()

    @ExceptionHandler(WebExchangeBindException::class)
    protected fun handleMethodArgumentNotValid(ex: WebExchangeBindException): ResponseEntity<Any> {
        logger.debug("Validation error: {}", ex.message)
        metrics?.record(ex, HttpStatus.BAD_REQUEST)
        return ApiError.of(HttpStatus.BAD_REQUEST, "Validation error")
            .apply {
                addValidationErrors(ex.bindingResult.fieldErrors)
                addValidationError(ex.bindingResult.globalErrors)
            }
            .toResponse()
    }

    @ExceptionHandler(ConstraintViolationException::class)
    protected fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<Any> {
        logger.debug("Constraint violation: {}", ex.message)
        metrics?.record(ex, HttpStatus.BAD_REQUEST)
        return ApiError.of(HttpStatus.BAD_REQUEST, "Validation error")
            .apply { addValidationErrors(ex.constraintViolations) }
            .toResponse()
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    protected fun handleMethodArgumentTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<Any> {
        logger.debug("Method argument type mismatch: parameter={}, value={}, requiredType={}",
            ex.name, ex.value, ex.requiredType?.simpleName)
        metrics?.record(ex, HttpStatus.BAD_REQUEST)
        return ApiError.of(HttpStatus.BAD_REQUEST, "The parameter ${ex.name} of value ${ex.value} " +
                "could not be converted to type ${ex.requiredType?.simpleName}")
            .toResponse()
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    protected fun handleDataIntegrityViolation(ex: DataIntegrityViolationException): ResponseEntity<Any> {
        logger.debug("Data integrity violation: {}", ex.message)
        metrics?.record(ex, HttpStatus.CONFLICT)
        return ApiError.of(HttpStatus.CONFLICT, "A conflict with a unique field was found")
            .toResponse()
    }

    @ExceptionHandler(ServerWebInputException::class)
    protected fun handleServerWebInput(ex: ServerWebInputException): ResponseEntity<Any> {
        logger.debug("Server web input error: {}", ex.reason)
        metrics?.record(ex, HttpStatus.BAD_REQUEST)
        return ApiError.of(HttpStatus.BAD_REQUEST, ex.reason ?: "Invalid input")
            .toResponse()
    }

    @ExceptionHandler(ResponseStatusException::class)
    protected fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<Any> {
        val httpStatus = HttpStatus.valueOf(ex.statusCode.value())
        logger.debug("Response status exception: status={}, reason={}", httpStatus, ex.reason)
        metrics?.record(ex, httpStatus)
        return ApiError.of(httpStatus, ex.reason ?: httpStatus.reasonPhrase)
            .toResponse()
    }

    @ExceptionHandler(TimeoutException::class)
    protected fun handleTimeoutExceptions(ex: TimeoutException): ResponseEntity<Any> {
        logger.debug("Timeout exception: {}", ex.message)
        metrics?.record(ex, HttpStatus.REQUEST_TIMEOUT)
        return ApiError.of(HttpStatus.REQUEST_TIMEOUT, ex.message ?: "Request timeout")
            .toResponse()
    }

    @ExceptionHandler(BaseException::class)
    protected fun handleBaseExceptions(ex: BaseException): ResponseEntity<Any> {
        val httpStatus = HttpStatus.valueOf(ex.statusCode)
        logger.debug("{}: {}", ex.javaClass.simpleName, ex.message)
        metrics?.record(ex, httpStatus)
        return ApiError.of(
            httpStatus,
            ex.message ?: httpStatus.reasonPhrase,
            code = ex.code,
            source = sourceResolver.resolve(ex),
            metadata = ex.metadata,
        ).toResponse()
    }

    @ExceptionHandler(Exception::class)
    protected fun handleOtherExceptions(ex: Exception): ResponseEntity<Any> {
        logger.error("Unhandled exception: ${ex.javaClass.simpleName} - ${ex.message}", ex)
        metrics?.record(ex, HttpStatus.INTERNAL_SERVER_ERROR)
        return ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", source = sourceResolver.resolveDefault())
            .toResponse()
    }

    private fun ApiError.toResponse(): ResponseEntity<Any> =
        ResponseEntity.status(status).contentType(PROBLEM_JSON).body(this)
}
