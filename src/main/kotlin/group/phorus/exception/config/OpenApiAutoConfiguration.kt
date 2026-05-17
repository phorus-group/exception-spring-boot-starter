package group.phorus.exception.config

import group.phorus.exception.handlers.ApiError
import group.phorus.exception.handlers.ValidationError
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.links.Link
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.customizers.OperationCustomizer
import org.springdoc.core.customizers.ParameterCustomizer
import org.springdoc.core.customizers.PropertyCustomizer
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.method.HandlerMethod
import java.util.concurrent.CopyOnWriteArrayList
import io.swagger.v3.oas.models.parameters.RequestBody as OasRequestBody

private const val API_ERROR_SCHEMA_NAME = "ApiError"
private const val VALIDATION_ERROR_SCHEMA_NAME = "ValidationError"
private const val API_ERROR_REF = "#/components/schemas/$API_ERROR_SCHEMA_NAME"
private const val SCHEMA_REF_PREFIX = "#/components/schemas/"
private val API_ERROR_REQUIRED_FIELDS = listOf("status", "title", "code", "timestamp")
private val VALIDATION_ERROR_REQUIRED_FIELDS = listOf("obj")
private const val X_VALIDATIONS_EXTENSION = "x-validations"
private const val X_VALIDATIONS_ENTRY_GROUPS = "groups"
private const val X_VALIDATIONS_ENTRY_RULE = "rule"
private const val X_VALIDATIONS_ENTRY_CODE = "code"
private const val X_VALIDATIONS_ENTRY_VALUE = "value"

private const val JAKARTA_PKG = "jakarta.validation.constraints"
private const val HIBERNATE_PKG = "org.hibernate.validator.constraints"

private val PRESENCE_RULES = setOf("notBlank", "required", "minLength", "minItems")

/**
 * Type of single `x-validations` array entry as it lives in the OpenAPI schema. Keys
 * include the public `rule` / `code` shape and an internal `groups` list (simple class
 * names of the Jakarta `groups` attribute on the source annotation) that gets stripped
 * before the spec is served. The latter lets [groupAwareOperationCustomizer] and
 * [groupedSchemaCustomizer] scope constraints to the active validation group.
 */
private typealias ValidationEntry = MutableMap<String, Any>

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
 * entry per recognized Jakarta constraint as the `x-validations` array. Each entry has
 * shape `{ "rule": "<name>", "code": "<RESERVED_CODE>" }`. The `rule` name follows
 * JSON Schema vocabulary where one exists (`minLength`, `maxItems`, `minimum`, `pattern`,
 * `format`) and falls back to a synthetic name otherwise (`required`, `notBlank`,
 * `assertTrue`). The `code` is the reserved fallback from `ReservedErrorCodes` for the
 * failing constraint type.
 *
 * Jakarta constraints carrying a non-empty `groups` attribute receive special handling:
 * when a controller pins the request body to a specific group via `@Validated(Group::class)`,
 * the operation's body schema `$ref` is rewritten to a group-specific clone whose
 * `x-validations` and `required` entries cover only that group. Operations without
 * `@Validated` (the default group) keep the original component.
 */
@AutoConfiguration
@EnableConfigurationProperties(ExceptionProperties::class)
@ConditionalOnClass(name = ["org.springdoc.core.customizers.OpenApiCustomizer"])
@ConditionalOnProperty(
    prefix = "group.phorus.exception.openapi",
    name = ["enabled"],
    matchIfMissing = true,
)
class OpenApiAutoConfiguration {

    /**
     * Bindings between an operation and the validation group set its `@RequestBody`
     * parameter pins via `@Validated(...)`. Collected by [groupAwareOperationCustomizer]
     * during the operation phase and consumed by [groupedSchemaCustomizer] in the
     * post-build phase. We keep the live [Operation] reference (not the request-body
     * schema's `$ref`) because springdoc has not yet registered the referenced component
     * in `components.schemas` when the operation customizer fires; the post-build
     * customizer reads the now-resolved ref, clones the component, and rewrites the ref
     * in place. Backed by a [CopyOnWriteArrayList] so the list is
     * safe to iterate while springdoc continues to register operations on other threads.
     */
    private val operationGroupBindings: MutableList<OperationGroupBinding> = CopyOnWriteArrayList()

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

            openApi.components.schemas[API_ERROR_SCHEMA_NAME]
                ?.required = API_ERROR_REQUIRED_FIELDS.toMutableList()
            openApi.components.schemas[VALIDATION_ERROR_SCHEMA_NAME]
                ?.required = VALIDATION_ERROR_REQUIRED_FIELDS.toMutableList()

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
    @ConditionalOnProperty(
        prefix = "group.phorus.exception.openapi.x-validations",
        name = ["enabled"],
        matchIfMissing = true,
    )
    fun validationsPropertyCustomizer(): PropertyCustomizer =
        PropertyCustomizer { schema, type ->
            schema?.applyValidationsFrom(type?.ctxAnnotations?.toList())
            schema
        }

    /**
     * Emits the `x-validations` extension on path / query / header / cookie parameters
     * the same way [validationsPropertyCustomizer] does for body DTO fields. springdoc
     * routes parameter schemas through a separate SPI (`ParameterCustomizer`) that does
     * NOT invoke `PropertyCustomizer`, so the body emission path and the parameter
     * emission path must each carry their own customizer. The standard JSON Schema
     * validators (`minLength`, `minimum`, `pattern`, etc.) are still emitted by springdoc
     * natively next to the array.
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "group.phorus.exception.openapi.x-validations",
        name = ["enabled"],
        matchIfMissing = true,
    )
    fun validationsParameterCustomizer(): ParameterCustomizer =
        ParameterCustomizer { parameterModel, methodParameter ->
            parameterModel?.schema?.applyValidationsFrom(methodParameter?.parameterAnnotations?.toList())
            parameterModel
        }

    /**
     * Inspects every operation's `@RequestBody` parameter for an `@Validated(Group::class)`
     * annotation and, when one or more groups are pinned, rewrites the request body's schema
     * `$ref` to a derived component name (e.g. `OriginalDto_CreateGroup`). The actual
     * derived component is materialized later by [groupedSchemaCustomizer], which has
     * access to the full OpenAPI document and the original component to clone.
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "group.phorus.exception.openapi.x-validations",
        name = ["enabled"],
        matchIfMissing = true,
    )
    fun groupAwareOperationCustomizer(): OperationCustomizer =
        OperationCustomizer { operation, handlerMethod ->
            val groups = handlerMethod.requestBodyValidationGroups() ?: return@OperationCustomizer operation
            if (groups.isNotEmpty()) {
                operationGroupBindings += OperationGroupBinding(operation, groups.toSortedSet().toList())
            }
            operation
        }

    /**
     * Materializes the per-group component clones recorded by [groupAwareOperationCustomizer]
     * and strips the internal `groups` key from every `x-validations` entry on the spec.
     * Runs after springdoc default OpenAPI generation pipeline.
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "group.phorus.exception.openapi.x-validations",
        name = ["enabled"],
        matchIfMissing = true,
    )
    fun groupedSchemaCustomizer(): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            val derivedComponentNames = applyGroupedSchemaRewrites(openApi)
            filterOriginalsToDefaultGroupView(openApi, derivedComponentNames)
            pruneUnreferencedComponents(openApi)
            stripInternalGroupsFromAllSchemas(openApi)
        }

    private fun Schema<*>.applyValidationsFrom(annotations: List<Annotation>?) {
        if (annotations.isNullOrEmpty()) return
        val entries = mutableListOf<ValidationEntry>()
        val seenRules = mutableSetOf<String>()

        for (annotation in annotations) {
            val canonical = annotation.annotationClass.java.canonicalName ?: continue
            applyJsonSchemaFor(canonical, annotation)
            entriesFor(canonical, annotation, this).forEach { entry ->
                if (seenRules.add(entry[X_VALIDATIONS_ENTRY_RULE] as String)) entries += entry
            }
        }

        if (entries.isNotEmpty()) {
            addExtension(X_VALIDATIONS_EXTENSION, entries)
        }
    }

    /**
     * Sets the JSON Schema validator keys (`minLength`, `minimum`, `exclusiveMinimum`, etc.)
     * for constraint annotations that springdoc default property converter does not
     * translate. The customizer runs after springdoc, so these calls fill the gaps without
     * overwriting values already populated for natively supported annotations.
     *
     * Skips annotations carrying a non-empty `groups()` attribute. Springdoc-native
     * annotations (`@Size`, `@Pattern`, ...) are already group-aware on emission, so the
     * served original carries no validators for group-scoped constraints; matching that
     * behavior here keeps the default view consistent. Per-group clones re-derive the
     * validators from the kept `x-validations` entries during [scopeSchemaToActiveGroups].
     */
    private fun Schema<*>.applyJsonSchemaFor(canonical: String, annotation: Annotation) {
        if (annotation.simpleGroupNames().isNotEmpty()) return
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

    private fun entriesFor(canonical: String, annotation: Annotation, schema: Schema<*>): List<ValidationEntry> {
        val groups = annotation.simpleGroupNames()
        return when (canonical) {
            "$JAKARTA_PKG.NotNull" -> listOf(rule("required", "REQUIRED", groups))
            "$JAKARTA_PKG.NotEmpty" -> listOf(rule(minLengthRuleFor(schema), "REQUIRED", groups, value = 1))
            "$JAKARTA_PKG.NotBlank" -> listOf(rule("notBlank", "BLANK", groups))
            "$JAKARTA_PKG.Null" -> listOf(rule("null", "MUST_BE_NULL", groups))
            "$JAKARTA_PKG.Size", "$HIBERNATE_PKG.Length" ->
                sizeEntries(annotation, schema, code = "TOO_SHORT" to "TOO_LONG", groups = groups)
            "$JAKARTA_PKG.Min" -> listOf(rule("minimum", "TOO_SMALL", groups, value = annotation.longAttribute("value", default = 0L)))
            "$JAKARTA_PKG.Max" -> listOf(rule("maximum", "TOO_LARGE", groups, value = annotation.longAttribute("value", default = 0L)))
            "$JAKARTA_PKG.DecimalMin" -> listOf(rule("minimum", "TOO_SMALL", groups, value = annotation.stringAttribute("value")))
            "$JAKARTA_PKG.DecimalMax" -> listOf(rule("maximum", "TOO_LARGE", groups, value = annotation.stringAttribute("value")))
            "$HIBERNATE_PKG.Range" -> rangeEntries(annotation, groups)
            "$JAKARTA_PKG.Positive" -> listOf(rule("exclusiveMinimum", "MUST_BE_POSITIVE", groups))
            "$JAKARTA_PKG.PositiveOrZero" -> listOf(rule("minimum", "MUST_BE_POSITIVE_OR_ZERO", groups))
            "$JAKARTA_PKG.Negative" -> listOf(rule("exclusiveMaximum", "MUST_BE_NEGATIVE", groups))
            "$JAKARTA_PKG.NegativeOrZero" -> listOf(rule("maximum", "MUST_BE_NEGATIVE_OR_ZERO", groups))
            "$JAKARTA_PKG.Digits" -> listOf(rule("digits", "INVALID_NUMBER_FORMAT", groups))
            "$JAKARTA_PKG.Pattern" -> listOf(rule("pattern", "INVALID_FORMAT", groups, value = annotation.stringAttribute("regexp")))
            "$JAKARTA_PKG.Email" -> listOf(rule("format", "INVALID_EMAIL", groups, value = "email"))
            "$JAKARTA_PKG.Past" -> listOf(rule("past", "MUST_BE_PAST", groups))
            "$JAKARTA_PKG.PastOrPresent" -> listOf(rule("pastOrPresent", "MUST_BE_PAST_OR_PRESENT", groups))
            "$JAKARTA_PKG.Future" -> listOf(rule("future", "MUST_BE_FUTURE", groups))
            "$JAKARTA_PKG.FutureOrPresent" -> listOf(rule("futureOrPresent", "MUST_BE_FUTURE_OR_PRESENT", groups))
            "$JAKARTA_PKG.AssertTrue" -> listOf(rule("assertTrue", "MUST_BE_TRUE", groups))
            "$JAKARTA_PKG.AssertFalse" -> listOf(rule("assertFalse", "MUST_BE_FALSE", groups))
            else -> emptyList()
        }
    }

    private fun sizeEntries(
        annotation: Annotation,
        schema: Schema<*>,
        code: Pair<String, String>,
        groups: List<String>,
    ): List<ValidationEntry> {
        val (lowerCode, upperCode) = code
        val min = annotation.intAttribute("min", default = 0)
        val max = annotation.intAttribute("max", default = Int.MAX_VALUE)
        val (lowerRule, upperRule) = boundaryRulesFor(schema)
        val entries = mutableListOf<ValidationEntry>()
        if (min > 0) entries += rule(lowerRule, lowerCode, groups, value = min)
        if (max < Int.MAX_VALUE) entries += rule(upperRule, upperCode, groups, value = max)
        return entries
    }

    private fun rangeEntries(annotation: Annotation, groups: List<String>): List<ValidationEntry> {
        // @Range is meta-annotated @Min(0) @Max(Long.MAX_VALUE), so the lower bound is
        // always validated (default 0 rejects negative numbers). The maximum entry is
        // only emitted when the user departed from the effectively-unbounded default.
        val min = annotation.longAttribute("min", default = 0L)
        val max = annotation.longAttribute("max", default = Long.MAX_VALUE)
        val entries = mutableListOf(rule("minimum", "TOO_SMALL", groups, value = min))
        if (max < Long.MAX_VALUE) entries += rule("maximum", "TOO_LARGE", groups, value = max)
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

    private fun rule(name: String, code: String, groups: List<String>, value: Any? = null): ValidationEntry =
        linkedMapOf<String, Any>(
            X_VALIDATIONS_ENTRY_RULE to name,
            X_VALIDATIONS_ENTRY_CODE to code,
            X_VALIDATIONS_ENTRY_GROUPS to groups,
        ).apply { if (value != null) put(X_VALIDATIONS_ENTRY_VALUE, value) }

    /**
     * Reads the `groups()` attribute from a Jakarta constraint annotation via reflection and
     * returns the simple class names. An empty list signals "default group", which matches
     * any active validation group on the consuming controller.
     */
    private fun Annotation.simpleGroupNames(): List<String> = try {
        @Suppress("UNCHECKED_CAST")
        val raw = annotationClass.java.getMethod("groups").invoke(this) as? Array<Class<*>>
        raw?.map { it.simpleName ?: it.name }.orEmpty()
    } catch (_: NoSuchMethodException) {
        emptyList()
    }

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

    private fun Annotation.stringAttribute(name: String): String? =
        try {
            annotationClass.java.getMethod(name).invoke(this) as? String
        } catch (_: NoSuchMethodException) {
            null
        }

    private data class OperationGroupBinding(val operation: Operation, val groups: List<String>)

    /**
     * Resolves the validation groups pinned for an operation's `@RequestBody` parameter.
     *
     * Returns:
     *  - `null` when the operation has no `@RequestBody` parameter (nothing to clone).
     *  - empty list when the operation has a body but no `@Validated` pin anywhere (default group).
     *  - non-empty list of simple group class names when `@Validated(Group::class)` is found on
     *    the parameter, the controller method, or the controller class. Spring's
     *    `MethodValidationInterceptor` falls back from parameter to method to class when
     *    resolving the active group, so the OpenAPI emission walks the same chain.
     */
    private fun HandlerMethod.requestBodyValidationGroups(): List<String>? {
        for (parameter in methodParameters) {
            if (parameter.hasParameterAnnotation(RequestBody::class.java)) {
                val paramValidated = parameter.getParameterAnnotation(Validated::class.java)
                if (paramValidated != null) return paramValidated.value.toGroupNames()
                val methodValidated = method.getAnnotation(Validated::class.java)
                if (methodValidated != null) return methodValidated.value.toGroupNames()
                val classValidated = beanType.getAnnotation(Validated::class.java)
                if (classValidated != null) return classValidated.value.toGroupNames()
                return emptyList()
            }
        }
        return null
    }

    private fun Array<kotlin.reflect.KClass<*>>.toGroupNames(): List<String> =
        map { it.simpleName ?: it.qualifiedName ?: it.toString() }

    /**
     * For each operation whose request body is pinned to one or more validation groups,
     * deep-clones the referenced component schema, scopes its `x-validations` and
     * `required` to the active groups, registers the clone under a derived name, and
     * rewrites the operation's request body `$ref` to point at the clone. Runs in the
     * post-build phase so the original component is already registered in `components.schemas`.
     */
    private fun applyGroupedSchemaRewrites(openApi: OpenAPI): Set<String> {
        val components = openApi.components ?: return emptySet()
        // Cache `(componentName, groupSet) -> derivedName` across the whole pass so two
        // operations pinned to the same group set share the same clone, and so a recursive
        // walk can't enter an infinite loop on self-referential DTOs.
        val derivedNames: MutableMap<Pair<String, List<String>>, String> = mutableMapOf()
        val needsCloneCache: MutableMap<String, Boolean> = mutableMapOf()

        operationGroupBindings.forEach { binding ->
            val mediaTypes = binding.operation.requestBody?.content?.values ?: return@forEach
            mediaTypes.forEach { mediaType ->
                val schemaNode = mediaType.schema ?: return@forEach
                val originalRef = schemaNode.`$ref` ?: return@forEach
                val componentName = originalRef.removePrefix(SCHEMA_REF_PREFIX)
                val derivedName = ensureGroupClone(components, componentName, binding.groups, derivedNames, needsCloneCache)
                    ?: return@forEach
                // Replace the schema on the mediaType with a fresh `$ref` holder. springdoc
                // reuses the same Schema instance across operations that take the same DTO
                // with the same parameter-level annotation set; mutating its `$ref` in place
                // would leak the rewrite into sibling operations.
                mediaType.schema = Schema<Any>().`$ref`("$SCHEMA_REF_PREFIX$derivedName")
            }
        }
        return derivedNames.values.toSet()
    }

    /**
     * Filters every non-derived component to its `Default`-group view. JSR 380 maps a `@Valid`
     * consumer (no `@Validated`) to the `Default` group, and constraints with an explicit
     * `groups()` attribute are not part of `Default`, so the served original must drop them
     * to match what the BE actually enforces. springdoc already respects `groups()` when it
     * derives the `required` array; this method does the equivalent for `x-validations`.
     *
     * Derived names produced by [applyGroupedSchemaRewrites] are skipped, those clones were
     * already filtered to their own active groups.
     */
    private fun filterOriginalsToDefaultGroupView(openApi: OpenAPI, derivedComponentNames: Set<String>) {
        val schemas = openApi.components?.schemas ?: return
        schemas.forEach { (name, schema) ->
            if (name !in derivedComponentNames) {
                scopeSchemaToActiveGroups(schema, emptySet())
            }
        }
    }

    /**
     * Drops components from `components.schemas` that no operation parameter, request body,
     * response, or other component transitively references. The walk starts from operation
     * roots and follows `$ref` through every node that can hold one (`properties`, `items`,
     * `allOf`, `oneOf`, `anyOf`, `additionalProperties`).
     */
    private fun pruneUnreferencedComponents(openApi: OpenAPI) {
        val components = openApi.components ?: return
        val schemas = components.schemas ?: return
        val reachable = computeReachableComponentNames(openApi, schemas)
        val toRemove = schemas.keys.filter { it !in reachable }
        toRemove.forEach { schemas.remove(it) }
    }

    private fun computeReachableComponentNames(
        openApi: OpenAPI,
        schemas: Map<String, Schema<*>>,
    ): Set<String> {
        val reachable = mutableSetOf<String>()
        val toVisit = ArrayDeque<String>()

        forEachSchemaInDocument(openApi) { schema -> collectRefsFromSchema(schema, toVisit) }

        while (toVisit.isNotEmpty()) {
            val name = toVisit.removeFirst()
            if (!reachable.add(name)) continue
            schemas[name]?.let { collectRefsFromSchema(it, toVisit) }
        }
        return reachable
    }

    /**
     * Yields every [Schema] reachable from the OpenAPI document tree outside of
     * `components.schemas`. Covers root-level `paths` and `webhooks`, every
     * `components` map that can hold a schema (parameters, responses, requestBodies,
     * headers, callbacks, pathItems), and every nested schema-bearing position inside
     * an operation (parameters, requestBody, responses with headers and links, callbacks,
     * mediaType.encoding[*].headers). Both `Parameter` and `Header` are walked through
     * their `schema` and their alternate `content[*].schema` shape.
     */
    private fun forEachSchemaInDocument(openApi: OpenAPI, action: (Schema<*>) -> Unit) {
        openApi.paths?.values?.forEach { walkPathItem(it, action) }
        openApi.webhooks?.values?.forEach { walkPathItem(it, action) }
        openApi.components?.let { components ->
            components.pathItems?.values?.forEach { walkPathItem(it, action) }
            components.parameters?.values?.forEach { walkParameter(it, action) }
            components.responses?.values?.forEach { walkApiResponse(it, action) }
            components.requestBodies?.values?.forEach { walkRequestBody(it, action) }
            components.headers?.values?.forEach { walkHeader(it, action) }
            components.callbacks?.values?.forEach { callback ->
                callback.values?.forEach { walkPathItem(it, action) }
            }
            components.links?.values?.forEach { walkLink(it, action) }
        }
    }

    private fun walkPathItem(pathItem: PathItem, action: (Schema<*>) -> Unit) {
        pathItem.parameters?.forEach { walkParameter(it, action) }
        pathItem.readOperations().forEach { walkOperation(it, action) }
    }

    private fun walkOperation(operation: Operation, action: (Schema<*>) -> Unit) {
        operation.parameters?.forEach { walkParameter(it, action) }
        operation.requestBody?.let { walkRequestBody(it, action) }
        operation.responses?.values?.forEach { walkApiResponse(it, action) }
        operation.callbacks?.values?.forEach { callback ->
            callback.values?.forEach { walkPathItem(it, action) }
        }
    }

    private fun walkParameter(parameter: Parameter, action: (Schema<*>) -> Unit) {
        parameter.schema?.let(action)
        parameter.content?.values?.forEach { walkMediaType(it, action) }
    }

    private fun walkHeader(header: Header, action: (Schema<*>) -> Unit) {
        header.schema?.let(action)
        header.content?.values?.forEach { walkMediaType(it, action) }
    }

    private fun walkRequestBody(requestBody: OasRequestBody, action: (Schema<*>) -> Unit) {
        requestBody.content?.values?.forEach { walkMediaType(it, action) }
    }

    private fun walkApiResponse(response: ApiResponse, action: (Schema<*>) -> Unit) {
        response.headers?.values?.forEach { walkHeader(it, action) }
        response.content?.values?.forEach { walkMediaType(it, action) }
        response.links?.values?.forEach { walkLink(it, action) }
    }

    /**
     * `Link.headers` is `@Deprecated` in swagger ("not part of OpenAPI specification") but
     * still serializable, so we walk it defensively for callers that hand-construct or use
     * the deprecated field; the suppression silences the per-call warning.
     */
    @Suppress("DEPRECATION")
    private fun walkLink(link: Link, action: (Schema<*>) -> Unit) {
        link.headers?.values?.forEach { walkHeader(it, action) }
    }

    private fun walkMediaType(mediaType: MediaType, action: (Schema<*>) -> Unit) {
        mediaType.schema?.let(action)
        mediaType.encoding?.values?.forEach { encoding ->
            encoding.headers?.values?.forEach { walkHeader(it, action) }
        }
    }

    private fun collectRefsFromSchema(schema: Schema<*>, acc: ArrayDeque<String>) {
        walkSubSchemas(schema) { sub ->
            sub.`$ref`?.removePrefix(SCHEMA_REF_PREFIX)?.let { acc += it }
            sub.discriminator?.mapping?.values?.forEach { value ->
                // Discriminator mapping values are either a full `#/components/schemas/<name>`
                // ref or a bare type identifier that resolves to the same component name.
                acc += value.removePrefix(SCHEMA_REF_PREFIX)
            }
        }
    }

    /**
     * Produces (or reuses) a per-group clone of a component schema. The clone's
     * `x-validations` and `required` are filtered to [groups]; any nested property
     * whose `$ref` points at a component carrying group-scoped constraints (directly
     * or transitively) is also cloned, recursively, and the clone's `$ref` is rewritten
     * in place. JSR 380 §5.4.5 propagates the active group through `@Valid` cascading
     * so the nested clones share the same active group set as their parent.
     */
    private fun ensureGroupClone(
        components: io.swagger.v3.oas.models.Components,
        componentName: String,
        groups: List<String>,
        derivedNames: MutableMap<Pair<String, List<String>>, String>,
        needsCloneCache: MutableMap<String, Boolean>,
    ): String? {
        val cacheKey = componentName to groups
        derivedNames[cacheKey]?.let { return it }

        val original = components.schemas?.get(componentName) ?: return null
        val derivedName = "${componentName}_${groups.joinToString("_")}"
        // Reserve the name BEFORE recursing so a cycle (DTO referencing itself) terminates
        // by hitting this cache entry instead of looping.
        derivedNames[cacheKey] = derivedName

        val clone = original.deepCloneViaJson() ?: return null
        scopeSchemaToActiveGroups(clone, groups.toSet())
        rewriteNestedRefsForGroups(clone, components, groups, derivedNames, needsCloneCache)

        components.addSchemas(derivedName, clone)
        return derivedName
    }

    /**
     * For every `$ref` reachable from [schema] (directly or through `properties`, `items`,
     * `allOf`, `oneOf`, `anyOf`, `additionalProperties`) that resolves to a component
     * needing group-scoped cloning, produces the inner clone via [ensureGroupClone] and
     * rewrites the `$ref` to point at it. Refs to fully ungrouped components are left
     * untouched so the original component is reused across endpoints. Container shapes
     * like `List<Inner>` (`items.$ref`), `Map<String, Inner>` (`additionalProperties.$ref`),
     * and polymorphic `oneOf`/`anyOf`/`allOf` unions are all walked.
     */
    private fun rewriteNestedRefsForGroups(
        schema: Schema<*>,
        components: io.swagger.v3.oas.models.Components,
        groups: List<String>,
        derivedNames: MutableMap<Pair<String, List<String>>, String>,
        needsCloneCache: MutableMap<String, Boolean>,
    ) {
        walkSubSchemas(schema) { sub ->
            val ref = sub.`$ref` ?: return@walkSubSchemas
            val innerName = ref.removePrefix(SCHEMA_REF_PREFIX)
            if (!componentNeedsGroupClone(components, innerName, needsCloneCache)) return@walkSubSchemas
            val innerDerived = ensureGroupClone(components, innerName, groups, derivedNames, needsCloneCache)
                ?: return@walkSubSchemas
            sub.`$ref` = "$SCHEMA_REF_PREFIX$innerDerived"
        }
    }

    /**
     * Visits every sub-schema reachable from [schema], including the schema itself, its
     * `properties.values`, its `items`, its `allOf`/`oneOf`/`anyOf` entries, and its
     * `additionalProperties` when that's a `Schema`. Used both by the cloning walker and
     * by `componentNeedsGroupClone` to inspect every place that can carry a `$ref` or a
     * group-scoped `x-validations` entry.
     */
    private fun walkSubSchemas(schema: Schema<*>, action: (Schema<*>) -> Unit) {
        action(schema)
        schema.properties?.values?.forEach { walkSubSchemas(it, action) }
        schema.items?.let { walkSubSchemas(it, action) }
        schema.allOf?.forEach { walkSubSchemas(it, action) }
        schema.oneOf?.forEach { walkSubSchemas(it, action) }
        schema.anyOf?.forEach { walkSubSchemas(it, action) }
        (schema.additionalProperties as? Schema<*>)?.let { walkSubSchemas(it, action) }
        schema.not?.let { walkSubSchemas(it, action) }
        schema.patternProperties?.values?.forEach { walkSubSchemas(it, action) }
        schema.prefixItems?.forEach { walkSubSchemas(it, action) }
        schema.propertyNames?.let { walkSubSchemas(it, action) }
        schema.contentSchema?.let { walkSubSchemas(it, action) }
        schema.contains?.let { walkSubSchemas(it, action) }
        (schema.unevaluatedProperties as? Schema<*>)?.let { walkSubSchemas(it, action) }
        schema.additionalItems?.let { walkSubSchemas(it, action) }
        schema.unevaluatedItems?.let { walkSubSchemas(it, action) }
        schema.`if`?.let { walkSubSchemas(it, action) }
        schema.`else`?.let { walkSubSchemas(it, action) }
        schema.then?.let { walkSubSchemas(it, action) }
        schema.dependentSchemas?.values?.forEach { walkSubSchemas(it, action) }
    }

    /**
     * Whether a component has any group-scoped validation that justifies a per-group clone.
     * A component qualifies when at least one of its own properties carries a non-empty
     * `groups` list on an `x-validations` entry, or when one of its `$ref`-typed properties
     * transitively reaches such a component. Memoized via [cache] to keep the walk
     * O(n) over the schema graph; cycles are broken via a per-call visited set.
     */
    private fun componentNeedsGroupClone(
        components: io.swagger.v3.oas.models.Components,
        componentName: String,
        cache: MutableMap<String, Boolean>,
        visited: MutableSet<String> = mutableSetOf(),
    ): Boolean {
        cache[componentName]?.let { return it }
        if (!visited.add(componentName)) return false
        val component = components.schemas?.get(componentName)
        if (component == null) {
            cache[componentName] = false
            return false
        }
        var result = false
        walkSubSchemas(component) { sub ->
            if (result) return@walkSubSchemas
            if (sub.hasGroupScopedValidation()) { result = true; return@walkSubSchemas }
            val innerRef = sub.`$ref`?.removePrefix(SCHEMA_REF_PREFIX) ?: return@walkSubSchemas
            if (innerRef == componentName) return@walkSubSchemas
            if (componentNeedsGroupClone(components, innerRef, cache, visited)) result = true
        }
        cache[componentName] = result
        return result
    }

    private fun Schema<*>.hasGroupScopedValidation(): Boolean {
        val entries = extensions?.get(X_VALIDATIONS_EXTENSION) as? List<*> ?: return false
        return entries.any { raw ->
            val entry = raw as? Map<*, *> ?: return@any false
            val entryGroups = entry[X_VALIDATIONS_ENTRY_GROUPS] as? List<*> ?: emptyList<Any>()
            entryGroups.isNotEmpty()
        }
    }

    private fun scopeSchemaToActiveGroups(schema: Schema<*>, activeGroups: Set<String>) {
        val existingRequired = schema.required?.toSet().orEmpty()
        val newRequired = mutableListOf<String>()
        schema.properties?.forEach { (propName, propSchema) ->
            val hadAnyXValidations =
                (propSchema.extensions?.get(X_VALIDATIONS_EXTENSION) as? List<*>)?.isNotEmpty() == true
            val keptEntries = propSchema.filterValidationsForActiveGroups(activeGroups)
            applyJsonSchemaFromEntries(propSchema, keptEntries)
            val derivedFromKept = keptEntries.any { it[X_VALIDATIONS_ENTRY_RULE] in PRESENCE_RULES }
            when {
                derivedFromKept -> newRequired += propName
                hadAnyXValidations -> Unit
                propName in existingRequired -> newRequired += propName
            }
        }
        schema.required = newRequired.takeIf { it.isNotEmpty() }
    }

    /**
     * Re-applies the JSON Schema validator keys (`minLength`, `pattern`, `minimum`, ...) on
     * [schema] from the entries kept by group filtering. Springdoc-native annotations
     * (`@Size`, `@Pattern`, ...) skip emission for group-scoped constraints, so per-group
     * clones inherit no validators from those even when the active group makes them apply.
     * This walk closes that gap by setting the validators from the entries' stored values.
     * Synthetic rules (`notBlank`, `assertTrue`, ...) and value-less entries
     * (`required` from `@NotNull`) have no JSON Schema equivalent and are skipped.
     */
    private fun applyJsonSchemaFromEntries(schema: Schema<*>, entries: List<ValidationEntry>) {
        entries.forEach { entry ->
            val rule = entry[X_VALIDATIONS_ENTRY_RULE] as? String ?: return@forEach
            val value = entry[X_VALIDATIONS_ENTRY_VALUE]
            when (rule) {
                "minLength" -> (value as? Int)?.let { schema.minLength = it }
                "maxLength" -> (value as? Int)?.let { schema.maxLength = it }
                "minItems" -> (value as? Int)?.let { schema.minItems = it }
                "maxItems" -> (value as? Int)?.let { schema.maxItems = it }
                "minProperties" -> (value as? Int)?.let { schema.minProperties = it }
                "maxProperties" -> (value as? Int)?.let { schema.maxProperties = it }
                "minimum" -> {
                    val bd = toBigDecimal(value) ?: java.math.BigDecimal.ZERO
                    schema.minimum = bd
                }
                "maximum" -> {
                    val bd = toBigDecimal(value) ?: java.math.BigDecimal.ZERO
                    schema.maximum = bd
                }
                "exclusiveMinimum" -> {
                    schema.minimum = java.math.BigDecimal.ZERO
                    schema.exclusiveMinimum = true
                    schema.exclusiveMinimumValue = java.math.BigDecimal.ZERO
                }
                "exclusiveMaximum" -> {
                    schema.maximum = java.math.BigDecimal.ZERO
                    schema.exclusiveMaximum = true
                    schema.exclusiveMaximumValue = java.math.BigDecimal.ZERO
                }
                "pattern" -> (value as? String)?.let { schema.pattern = it }
                "format" -> (value as? String)?.let { schema.format = it }
            }
        }
    }

    private fun toBigDecimal(value: Any?): java.math.BigDecimal? = when (value) {
        is java.math.BigDecimal -> value
        is Int -> java.math.BigDecimal.valueOf(value.toLong())
        is Long -> java.math.BigDecimal.valueOf(value)
        is Double -> java.math.BigDecimal.valueOf(value)
        is String -> runCatching { java.math.BigDecimal(value) }.getOrNull()
        else -> null
    }

    /**
     * Filters this property schema's `x-validations` array in place: keeps only entries
     * whose `groups` is empty (default group, applies to all) or contains one of the
     * [activeGroups]. Returns the kept entries so the caller can derive `required` from
     * presence rules without re-reading the schema.
     */
    private fun Schema<*>.filterValidationsForActiveGroups(activeGroups: Set<String>): List<ValidationEntry> {
        val entries = extensions?.get(X_VALIDATIONS_EXTENSION) as? MutableList<*> ?: return emptyList()
        val kept = entries.mapNotNull { raw ->
            @Suppress("UNCHECKED_CAST")
            val entry = raw as? ValidationEntry ?: return@mapNotNull null
            val entryGroups = entry[X_VALIDATIONS_ENTRY_GROUPS] as? List<*> ?: emptyList<Any>()
            val applies = entryGroups.isEmpty() || entryGroups.any { it in activeGroups }
            if (applies) entry else null
        }
        if (kept.isEmpty()) {
            extensions?.remove(X_VALIDATIONS_EXTENSION)
        } else {
            extensions?.set(X_VALIDATIONS_EXTENSION, kept.toMutableList())
        }
        return kept
    }

    /**
     * Strips the internal `groups` key from every `x-validations` entry on every schema
     * in the spec. The body [validationsPropertyCustomizer] and the parameter
     * [validationsParameterCustomizer] both attach the key as scratch state for the
     * group-aware cloning logic to filter on; it must not surface in the served document.
     * Walks `components.schemas` (body DTOs) and
     * `paths.{path}.{method}.parameters[].schema` (path / query / header / cookie params).
     */
    private fun stripInternalGroupsFromAllSchemas(openApi: OpenAPI) {
        openApi.components?.schemas?.values?.forEach { schema ->
            walkSubSchemas(schema) { it.stripInternalGroupsFromXValidations() }
        }
        forEachSchemaInDocument(openApi) { schema ->
            walkSubSchemas(schema) { it.stripInternalGroupsFromXValidations() }
        }
    }

    private fun Schema<*>.stripInternalGroupsFromXValidations() {
        val entries = extensions?.get(X_VALIDATIONS_EXTENSION) as? MutableList<*> ?: return
        entries.forEach { raw ->
            @Suppress("UNCHECKED_CAST")
            (raw as? ValidationEntry)?.let {
                it.remove(X_VALIDATIONS_ENTRY_GROUPS)
                it.remove(X_VALIDATIONS_ENTRY_VALUE)
            }
        }
    }

    /**
     * Deep-clones a swagger [Schema] via a JSON round-trip using swagger's own mapper, which
     * understands the polymorphic schema hierarchy. Returns `null` only if serialization fails,
     * in which case the caller skips the clone. Used to materialize group-specific component
     * variants without aliasing the original schema's `extensions` / `properties` maps.
     */
    private fun Schema<*>.deepCloneViaJson(): Schema<*>? {
        val mapper = Json.mapper()
        val json = mapper.writeValueAsString(this)
        val clone = mapper.readValue(json, Schema::class.java) ?: return null
        // The V30 SchemaMixin marks `getTypes` and several other JsonSchema (V31) fields
        // as @JsonIgnore, so the JSON roundtrip silently drops them. springdoc emits
        // JsonSchema (V31) instances whose primitive type lives in `types`, not `type`,
        // and whose `specVersion` defaults to V30 on deserialization. Without this sync
        // the clone serializes as `{type: object}` (the empty-type fallback) regardless
        // of the original's actual type.
        syncV31FieldsRecursively(this, clone)
        return clone
    }

    /**
     * Restores fields that the V30 [Json.mapper] silently drops during the JSON
     * roundtrip and replaces V31 schema-typed sub-schemas with independent deep
     * clones so the cloning rewrite never leaks into the original. The V30
     * [io.swagger.v3.core.jackson.mixin.SchemaMixin] marks every JSON Schema
     * 2020-12 field as `@JsonIgnore`, including the type indicator on
     * [io.swagger.v3.oas.models.media.JsonSchema] (`Set<String> types`) and
     * structural keywords like `contains`, `contentSchema`, `propertyNames`,
     * `patternProperties`, `prefixItems`, `if` / `then` / `else`,
     * `dependentSchemas`, `additionalItems`, `unevaluatedItems`. Sub-schemas
     * carried by V30 keywords (`properties`, `items`, `allOf`, `oneOf`, `anyOf`,
     * `additionalProperties`, `not`) survive the roundtrip and only need their
     * V31 children synced in place.
     */
    private fun syncV31FieldsRecursively(original: Schema<*>, clone: Schema<*>) {
        clone.specVersion = original.specVersion
        if (original.types != null) clone.types = original.types.toMutableSet()
        if (original.exclusiveMinimumValue != null) clone.exclusiveMinimumValue = original.exclusiveMinimumValue
        if (original.exclusiveMaximumValue != null) clone.exclusiveMaximumValue = original.exclusiveMaximumValue
        if (original.contentEncoding != null) clone.contentEncoding = original.contentEncoding
        if (original.contentMediaType != null) clone.contentMediaType = original.contentMediaType
        if (original.`$id` != null) clone.`$id` = original.`$id`
        if (original.`$anchor` != null) clone.`$anchor` = original.`$anchor`
        if (original.`$schema` != null) clone.`$schema` = original.`$schema`
        if (original.maxContains != null) clone.maxContains = original.maxContains
        if (original.minContains != null) clone.minContains = original.minContains
        if (original.`$comment` != null) clone.`set$comment`(original.`$comment`)
        original.getConst()?.let { clone.setConst(it) }
        if (original.booleanSchemaValue != null) clone.booleanSchemaValue = original.booleanSchemaValue
        // examples is `List<T>`; with `Schema<*>` the type parameter is unknown, so we
        // route through a star-projected assignment via the raw setter.
        if (original.examples != null) {
            @Suppress("UNCHECKED_CAST")
            (clone as Schema<Any?>).examples = (original.examples as List<Any?>).toMutableList()
        }
        if (original.dependentRequired != null) {
            clone.dependentRequired = original.dependentRequired
                .mapValuesTo(mutableMapOf()) { (_, v) -> v.toMutableList() }
        }

        // V31 schema-typed fields: deep-clone from the original so subsequent
        // mutation of the clone (e.g. group-aware `$ref` rewriting) cannot leak
        // into the original instance.
        clone.patternProperties = original.patternProperties?.mapValuesTo(mutableMapOf()) { (_, v) -> v.deepCloneViaJson()!! }
        clone.dependentSchemas = original.dependentSchemas?.mapValuesTo(mutableMapOf()) { (_, v) -> v.deepCloneViaJson()!! }
        clone.prefixItems = original.prefixItems?.mapTo(mutableListOf()) { it.deepCloneViaJson()!! }
        clone.contentSchema = original.contentSchema?.deepCloneViaJson()
        clone.contains = original.contains?.deepCloneViaJson()
        clone.propertyNames = original.propertyNames?.deepCloneViaJson()
        clone.additionalItems = original.additionalItems?.deepCloneViaJson()
        clone.unevaluatedItems = original.unevaluatedItems?.deepCloneViaJson()
        clone.`if` = original.`if`?.deepCloneViaJson()
        clone.`else` = original.`else`?.deepCloneViaJson()
        clone.then = original.then?.deepCloneViaJson()
        (original.unevaluatedProperties as? Schema<*>)?.let {
            clone.unevaluatedProperties = it.deepCloneViaJson()
        }

        // V30 schema-typed fields already roundtripped; recurse to fix V31
        // children nested inside them.
        original.properties?.forEach { (name, originalProp) ->
            clone.properties?.get(name)?.let { syncV31FieldsRecursively(originalProp, it) }
        }
        original.items?.let { o -> clone.items?.let { c -> syncV31FieldsRecursively(o, c) } }
        original.allOf?.forEachIndexed { i, o -> clone.allOf?.getOrNull(i)?.let { syncV31FieldsRecursively(o, it) } }
        original.oneOf?.forEachIndexed { i, o -> clone.oneOf?.getOrNull(i)?.let { syncV31FieldsRecursively(o, it) } }
        original.anyOf?.forEachIndexed { i, o -> clone.anyOf?.getOrNull(i)?.let { syncV31FieldsRecursively(o, it) } }
        (original.additionalProperties as? Schema<*>)?.let { o ->
            (clone.additionalProperties as? Schema<*>)?.let { c -> syncV31FieldsRecursively(o, c) }
        }
        original.not?.let { o -> clone.not?.let { c -> syncV31FieldsRecursively(o, c) } }
    }
}
