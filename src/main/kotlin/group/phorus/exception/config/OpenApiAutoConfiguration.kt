package group.phorus.exception.config

import group.phorus.exception.handlers.ApiError
import group.phorus.exception.handlers.ValidationError
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.customizers.PropertyCustomizer
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean

private const val API_ERROR_SCHEMA_NAME = "ApiError"
private const val API_ERROR_REF = "#/components/schemas/$API_ERROR_SCHEMA_NAME"
private const val X_VALIDATIONS_EXTENSION = "x-validations"

private const val JAKARTA_PKG = "jakarta.validation.constraints"
private const val HIBERNATE_PKG = "org.hibernate.validator.constraints"

/**
 * Autoconfiguration that registers [ApiError] and [ValidationError] schemas in the OpenAPI
 * spec, adds a default error response to all operations, and publishes the failing
 * Jakarta constraints of every property as the `x-validations` schema extension.
 *
 * Only active when springdoc-openapi is on the classpath.
 *
 * The customizer registers [ApiError] and [ValidationError] as reusable component schemas
 * and adds a `default` response referencing the `ApiError` schema with content type
 * `application/problem+json` to every operation that does not already define one.
 *
 * The [PropertyCustomizer] bean walks the property's context annotations and emits one
 * entry per recognised Jakarta constraint as the `x-validations` array. Each entry has
 * shape `{ "rule": "<name>", "code": "<RESERVED_CODE>" }`. The `rule` name follows
 * JSON Schema vocabulary where one exists (`minLength`, `maxItems`, `minimum`, `pattern`,
 * `format`) and falls back to a synthetic name otherwise (`required`, `notBlank`,
 * `assertTrue`). The `code` is the reserved fallback from `ReservedErrorCodes` for the
 * failing constraint type.
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

    /**
     * Emits the `x-validations` extension on every property whose backing field carries
     * recognized Jakarta or Hibernate Validator constraint annotations.
     */
    @Bean
    fun validationsPropertyCustomizer(): PropertyCustomizer =
        PropertyCustomizer { schema, type ->
            schema?.applyValidationsFrom(type)
            schema
        }

    private fun Schema<*>.applyValidationsFrom(type: AnnotatedType?) {
        val annotations = type?.ctxAnnotations ?: return
        val entries = mutableListOf<Map<String, String>>()
        val seenRules = mutableSetOf<String>()

        for (annotation in annotations) {
            val canonical = annotation.annotationClass.java.canonicalName ?: continue
            applyJsonSchemaFor(canonical, annotation)
            entries.addAll(entriesFor(canonical, annotation, this).filter { seenRules.add(it["rule"]!!) })
        }

        if (entries.isNotEmpty()) {
            addExtension(X_VALIDATIONS_EXTENSION, entries)
        }
    }

    /**
     * Sets the JSON Schema validator keys (`minLength`, `minimum`, `exclusiveMinimum`, etc.)
     * for constraint annotations that springdoc's default property converter does not
     * translate. The customizer runs after springdoc, so these calls fill the gaps without
     * overwriting values already populated for natively supported annotations.
     */
    private fun Schema<*>.applyJsonSchemaFor(canonical: String, annotation: Annotation) {
        when (canonical) {
            "$HIBERNATE_PKG.Length" -> {
                val min = annotation.intAttribute("min", default = 0)
                val max = annotation.intAttribute("max", default = Int.MAX_VALUE)
                if (min > 0) minLength = min
                if (max < Int.MAX_VALUE) maxLength = max
            }
            "$HIBERNATE_PKG.Range" -> {
                val min = annotation.longAttribute("min", default = 0L)
                val max = annotation.longAttribute("max", default = Long.MAX_VALUE)
                minimum = java.math.BigDecimal.valueOf(min)
                if (max < Long.MAX_VALUE) maximum = java.math.BigDecimal.valueOf(max)
            }
            "$JAKARTA_PKG.Positive" -> {
                minimum = java.math.BigDecimal.ZERO
                exclusiveMinimum = true
                exclusiveMinimumValue = java.math.BigDecimal.ZERO
            }
            "$JAKARTA_PKG.PositiveOrZero" -> {
                minimum = java.math.BigDecimal.ZERO
            }
            "$JAKARTA_PKG.Negative" -> {
                maximum = java.math.BigDecimal.ZERO
                exclusiveMaximum = true
                exclusiveMaximumValue = java.math.BigDecimal.ZERO
            }
            "$JAKARTA_PKG.NegativeOrZero" -> {
                maximum = java.math.BigDecimal.ZERO
            }
        }
    }

    private fun entriesFor(canonical: String, annotation: Annotation, schema: Schema<*>): List<Map<String, String>> =
        when (canonical) {
            "$JAKARTA_PKG.NotNull" -> listOf(rule("required", "REQUIRED"))
            "$JAKARTA_PKG.NotEmpty" -> listOf(rule(minLengthRuleFor(schema), "REQUIRED"))
            "$JAKARTA_PKG.NotBlank" -> listOf(rule("notBlank", "BLANK"))
            "$JAKARTA_PKG.Null" -> listOf(rule("null", "MUST_BE_NULL"))
            "$JAKARTA_PKG.Size", "$HIBERNATE_PKG.Length" ->
                sizeEntries(annotation, schema, code = "TOO_SHORT" to "TOO_LONG")
            "$JAKARTA_PKG.Min", "$JAKARTA_PKG.DecimalMin" ->
                listOf(rule("minimum", "TOO_SMALL"))
            "$JAKARTA_PKG.Max", "$JAKARTA_PKG.DecimalMax" ->
                listOf(rule("maximum", "TOO_LARGE"))
            "$HIBERNATE_PKG.Range" -> rangeEntries(annotation)
            "$JAKARTA_PKG.Positive" -> listOf(rule("exclusiveMinimum", "MUST_BE_POSITIVE"))
            "$JAKARTA_PKG.PositiveOrZero" -> listOf(rule("minimum", "MUST_BE_POSITIVE_OR_ZERO"))
            "$JAKARTA_PKG.Negative" -> listOf(rule("exclusiveMaximum", "MUST_BE_NEGATIVE"))
            "$JAKARTA_PKG.NegativeOrZero" -> listOf(rule("maximum", "MUST_BE_NEGATIVE_OR_ZERO"))
            "$JAKARTA_PKG.Digits" -> listOf(rule("digits", "INVALID_NUMBER_FORMAT"))
            "$JAKARTA_PKG.Pattern" -> listOf(rule("pattern", "INVALID_FORMAT"))
            "$JAKARTA_PKG.Email" -> listOf(rule("format", "INVALID_EMAIL"))
            "$JAKARTA_PKG.Past" -> listOf(rule("past", "MUST_BE_PAST"))
            "$JAKARTA_PKG.PastOrPresent" -> listOf(rule("pastOrPresent", "MUST_BE_PAST_OR_PRESENT"))
            "$JAKARTA_PKG.Future" -> listOf(rule("future", "MUST_BE_FUTURE"))
            "$JAKARTA_PKG.FutureOrPresent" -> listOf(rule("futureOrPresent", "MUST_BE_FUTURE_OR_PRESENT"))
            "$JAKARTA_PKG.AssertTrue" -> listOf(rule("assertTrue", "MUST_BE_TRUE"))
            "$JAKARTA_PKG.AssertFalse" -> listOf(rule("assertFalse", "MUST_BE_FALSE"))
            else -> emptyList()
        }

    private fun sizeEntries(
        annotation: Annotation,
        schema: Schema<*>,
        code: Pair<String, String>,
    ): List<Map<String, String>> {
        val (lowerCode, upperCode) = code
        val min = annotation.intAttribute("min", default = 0)
        val max = annotation.intAttribute("max", default = Int.MAX_VALUE)
        val (lowerRule, upperRule) = boundaryRulesFor(schema)
        val entries = mutableListOf<Map<String, String>>()
        if (min > 0) entries += rule(lowerRule, lowerCode)
        if (max < Int.MAX_VALUE) entries += rule(upperRule, upperCode)
        return entries
    }

    private fun rangeEntries(annotation: Annotation): List<Map<String, String>> {
        // @Range is meta-annotated @Min(0) @Max(Long.MAX_VALUE), so the lower bound is
        // always validated (default 0 rejects negative numbers). The maximum entry is
        // only emitted when the user departed from the effectively-unbounded default.
        val max = annotation.longAttribute("max", default = Long.MAX_VALUE)
        val entries = mutableListOf(rule("minimum", "TOO_SMALL"))
        if (max < Long.MAX_VALUE) entries += rule("maximum", "TOO_LARGE")
        return entries
    }

    private fun boundaryRulesFor(schema: Schema<*>): Pair<String, String> = when (schema.type) {
        "array" -> "minItems" to "maxItems"
        "object" -> "minProperties" to "maxProperties"
        else -> "minLength" to "maxLength"
    }

    private fun minLengthRuleFor(schema: Schema<*>): String = when (schema.type) {
        "array" -> "minItems"
        "object" -> "minProperties"
        else -> "minLength"
    }

    private fun rule(name: String, code: String): Map<String, String> =
        linkedMapOf("rule" to name, "code" to code)

    private fun Annotation.intAttribute(name: String, default: Int): Int =
        try {
            (annotationClass.java.getMethod(name).invoke(this) as? Int) ?: default
        } catch (_: NoSuchMethodException) {
            default
        }

    private fun Annotation.longAttribute(name: String, default: Long): Long =
        try {
            (annotationClass.java.getMethod(name).invoke(this) as? Long) ?: default
        } catch (_: NoSuchMethodException) {
            default
        }
}
