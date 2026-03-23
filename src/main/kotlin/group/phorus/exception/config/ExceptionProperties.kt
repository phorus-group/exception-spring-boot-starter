package group.phorus.exception.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for the Phorus exception handling starter.
 *
 * ```yaml
 * group:
 *   phorus:
 *     exception:
 *       include-source: true  # default: true. Set to false to omit the source field from error responses.
 * ```
 *
 * When `include-source` is `true`, the `source` field in error responses is autopopulated
 * from `spring.application.name`. Exceptions that set `source` explicitly override this default.
 */
@ConfigurationProperties(prefix = "group.phorus.exception")
data class ExceptionProperties(
    val includeSource: Boolean = true,
)
