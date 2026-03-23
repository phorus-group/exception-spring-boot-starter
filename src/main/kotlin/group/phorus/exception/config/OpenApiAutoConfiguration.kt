package group.phorus.exception.config

import group.phorus.exception.handlers.ApiError
import group.phorus.exception.handlers.ValidationError
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.boot.autoconfigure.AutoConfiguration

private const val API_ERROR_SCHEMA_NAME = "ApiError"
private const val API_ERROR_REF = "#/components/schemas/$API_ERROR_SCHEMA_NAME"

/**
 * Autoconfiguration that registers [ApiError] and [ValidationError] schemas
 * in the OpenAPI spec and adds a default error response to all operations.
 *
 * Only active when springdoc-openapi is on the classpath.
 *
 * The customizer:
 * 1. Registers [ApiError] and [ValidationError] as reusable component schemas.
 * 2. Adds a `default` response referencing the `ApiError` schema with content type
 *    `application/problem+json` to every operation that does not already define one.
 */
@AutoConfiguration
@ConditionalOnClass(name = ["org.springdoc.core.customizers.OpenApiCustomizer"])
class OpenApiAutoConfiguration {

    /**
     * Creates an [OpenApiCustomizer] that registers error schemas and default error responses.
     */
    @Bean
    fun apiErrorSchemaAndResponses(): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            val schemas = ModelConverters.getInstance().read(ApiError::class.java)
            schemas.forEach { (name, schema) ->
                openApi.components.addSchemas(name, schema)
            }

            val validationErrorSchemas = ModelConverters.getInstance().read(ValidationError::class.java)
            validationErrorSchemas.forEach { (name, schema) ->
                openApi.components.addSchemas(name, schema)
            }

            val errorSchemaRef = Schema<ApiError>().`$ref`(API_ERROR_REF)

            val errorContent = Content().addMediaType(
                "application/problem+json",
                MediaType().schema(errorSchemaRef)
            )

            openApi.paths?.values?.forEach { pathItem ->
                pathItem.readOperations().forEach { operation ->
                    if (operation.responses["default"] == null) {
                        operation.responses.addApiResponse(
                            "default",
                            ApiResponse()
                                .description("Error")
                                .content(errorContent)
                        )
                    }
                }
            }
        }
}
