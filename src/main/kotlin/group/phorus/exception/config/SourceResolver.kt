package group.phorus.exception.config

import group.phorus.exception.core.BaseException
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties

/**
 * Resolves the `source` field for error responses.
 *
 * If the exception sets `source` explicitly, that value is used. Otherwise, when
 * `group.phorus.exception.include-source` is `true` (the default), the value of
 * `spring.application.name` is used. When disabled or when no application name is
 * configured, `source` is `null` (omitted from the response).
 */
@AutoConfiguration
@EnableConfigurationProperties(ExceptionProperties::class)
class SourceResolver(
    private val properties: ExceptionProperties,
    @Value("\${spring.application.name:}") private val applicationName: String,
) {
    /**
     * Returns the source identifier for the given [BaseException].
     *
     * Uses the exception's own [BaseException.source] if set; otherwise falls back to
     * `spring.application.name` when [ExceptionProperties.includeSource] is enabled.
     *
     * @param ex the exception to resolve the source for.
     * @return the source string, or `null` if no source is available.
     */
    fun resolve(ex: BaseException): String? =
        ex.source ?: if (properties.includeSource && applicationName.isNotBlank()) applicationName else null

    /**
     * Returns the default source identifier (the application name) when no exception-specific
     * source is available.
     *
     * Used for non-[BaseException] errors (e.g., unhandled exceptions, framework errors)
     * where the exception itself does not carry a source field.
     *
     * @return the application name, or `null` if [ExceptionProperties.includeSource] is disabled
     *         or no application name is configured.
     */
    fun resolveDefault(): String? =
        if (properties.includeSource && applicationName.isNotBlank()) applicationName else null
}
