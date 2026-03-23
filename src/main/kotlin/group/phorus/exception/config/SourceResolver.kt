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
    fun resolve(ex: BaseException): String? =
        ex.source ?: if (properties.includeSource && applicationName.isNotBlank()) applicationName else null

    fun resolveDefault(): String? =
        if (properties.includeSource && applicationName.isNotBlank()) applicationName else null
}
