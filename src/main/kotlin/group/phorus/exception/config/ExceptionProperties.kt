package group.phorus.exception.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

/**
 * Configuration properties for the Phorus exception handling starter.
 *
 * ```yaml
 * group:
 *   phorus:
 *     exception:
 *       include-source: true       # default: true. Omit the source field from error responses when false.
 *       openapi:
 *         enabled: true            # default: true. Disable the whole OpenAPI autoconfig when false.
 *         x-validations:
 *           enabled: true          # default: true. Disable x-validations + group cloning when false.
 *                                  # The ApiError / ValidationError schemas and the default error
 *                                  # response on every operation are still added.
 * ```
 *
 * `include-source` autopopulates the `source` field from `spring.application.name`. Exceptions
 * that set `source` explicitly override this default.
 *
 * `openapi.enabled = false` skips every OpenAPI customizer bean the library would register, so
 * a consumer that already publishes its own `ApiError` schema or its own validation extensions
 * can opt out cleanly.
 *
 * `openapi.x-validations.enabled = false` keeps the `ApiError` and `ValidationError` component
 * schemas plus the default `application/problem+json` response on every operation, but skips
 * the `x-validations` extension, the parameter-side equivalent, and the per-group schema
 * cloning. Useful when you want the error contract published but do not want SDK generators
 * to see the reserved-code mapping.
 */
@ConfigurationProperties(prefix = "group.phorus.exception")
data class ExceptionProperties(
    val includeSource: Boolean = true,

    @NestedConfigurationProperty
    val openapi: OpenapiProperties = OpenapiProperties(),
) {
    data class OpenapiProperties(
        val enabled: Boolean = true,

        @NestedConfigurationProperty
        val xValidations: XValidationsProperties = XValidationsProperties(),
    )

    data class XValidationsProperties(
        val enabled: Boolean = true,
    )
}
