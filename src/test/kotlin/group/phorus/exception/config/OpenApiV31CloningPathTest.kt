package group.phorus.exception.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.RequestBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Exercises the per-group cloning rewrite path through V31 sub-schema positions
 * (`patternProperties`, `prefixItems`, `contentSchema`, `propertyNames`, `not`,
 * `contains`, `if` / `then` / `else`, `dependentSchemas`, `additionalItems`,
 * `unevaluatedItems`). For each, a parent component holds a `$ref` to an inner
 * component carrying group-scoped `x-validations`, an operation pinned to that
 * group is recorded, and the per-group clone is expected to rewrite the inner
 * `$ref` to the cloned variant.
 *
 * `operationGroupBindings` is a private field on [OpenApiAutoConfiguration]
 * normally populated by `groupAwareOperationCustomizer` reading a `HandlerMethod`.
 * Tests bypass the operation customizer and inject the binding directly via
 * reflection so no Spring infrastructure is needed.
 */
class OpenApiV31CloningPathTest {

    private fun groupScopedInner(): Schema<*> = ObjectSchema().apply {
        properties = mutableMapOf(
            "name" to StringSchema().apply {
                addExtension(
                    "x-validations",
                    mutableListOf(
                        mutableMapOf<String, Any>(
                            "rule" to "notBlank",
                            "code" to "BLANK",
                            "groups" to listOf("CreateGroup"),
                        ),
                    ),
                )
            },
        )
    }

    private fun newCustomizerWithBinding(operation: Operation, groups: List<String>): OpenApiAutoConfiguration {
        val config = OpenApiAutoConfiguration()
        val field = OpenApiAutoConfiguration::class.java.getDeclaredField("operationGroupBindings")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val list = field.get(config) as MutableList<Any>
        val bindingClass = Class.forName("group.phorus.exception.config.OpenApiAutoConfiguration\$OperationGroupBinding")
        val ctor = bindingClass.declaredConstructors.first()
        ctor.isAccessible = true
        list += ctor.newInstance(operation, groups)
        return config
    }

    private fun specWithBinding(parent: Schema<*>, groups: List<String> = listOf("CreateGroup")): Pair<OpenAPI, OpenApiAutoConfiguration> {
        val operation = Operation().requestBody(
            RequestBody().content(
                Content().addMediaType(
                    "application/json",
                    MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/Parent")),
                ),
            ),
        )
        val openApi = OpenAPI().apply {
            components = Components()
            paths = Paths().addPathItem("/v1/x", PathItem().post(operation))
        }
        openApi.components.addSchemas("Parent", parent)
        openApi.components.addSchemas("Inner", groupScopedInner())
        val config = newCustomizerWithBinding(operation, groups)
        return openApi to config
    }

    private fun assertCloneRewroteRefIn(openApi: OpenAPI, locator: (Schema<*>) -> Schema<*>?) {
        val parentClone = openApi.components.schemas["Parent_CreateGroup"]
            ?: error("Expected Parent_CreateGroup to be registered. Schemas: ${openApi.components.schemas.keys}")
        assertThat(openApi.components.schemas).containsKey("Inner_CreateGroup")
        val rewritten = locator(parentClone)
            ?: error("Locator returned null for Parent_CreateGroup. Clone: $parentClone")
        assertThat(rewritten.`$ref`)
            .`as`("Expected the inner ref to be rewritten to the per-group clone")
            .isEqualTo("#/components/schemas/Inner_CreateGroup")
    }

    @Test
    fun `clone rewrites $ref inside patternProperties to the per-group inner clone`() {
        val parent = ObjectSchema().apply {
            patternProperties = mutableMapOf("^x-.*$" to Schema<Any>().`$ref`("#/components/schemas/Inner"))
        }
        val (openApi, config) = specWithBinding(parent)
        config.groupedSchemaCustomizer().customise(openApi)
        assertCloneRewroteRefIn(openApi) { it.patternProperties?.values?.first() }
    }

    @Test
    fun `clone rewrites $ref inside prefixItems`() {
        val parent = ArraySchema().apply {
            prefixItems = mutableListOf(Schema<Any>().`$ref`("#/components/schemas/Inner"))
        }
        val (openApi, config) = specWithBinding(parent)
        config.groupedSchemaCustomizer().customise(openApi)
        assertCloneRewroteRefIn(openApi) { it.prefixItems?.first() }
    }

    @Test
    fun `clone rewrites $ref inside contentSchema`() {
        val parent = ObjectSchema().apply {
            contentSchema = Schema<Any>().`$ref`("#/components/schemas/Inner")
        }
        val (openApi, config) = specWithBinding(parent)
        config.groupedSchemaCustomizer().customise(openApi)
        assertCloneRewroteRefIn(openApi) { it.contentSchema }
    }

    @Test
    fun `clone rewrites $ref inside propertyNames`() {
        val parent = ObjectSchema().apply {
            propertyNames = Schema<Any>().`$ref`("#/components/schemas/Inner")
        }
        val (openApi, config) = specWithBinding(parent)
        config.groupedSchemaCustomizer().customise(openApi)
        assertCloneRewroteRefIn(openApi) { it.propertyNames }
    }

    @Test
    fun `clone rewrites $ref inside not`() {
        val parent = ObjectSchema().apply { not = Schema<Any>().`$ref`("#/components/schemas/Inner") }
        val (openApi, config) = specWithBinding(parent)
        config.groupedSchemaCustomizer().customise(openApi)
        assertCloneRewroteRefIn(openApi) { it.not }
    }

    @Test
    fun `clone rewrites $ref inside contains`() {
        val parent = ArraySchema().apply { contains = Schema<Any>().`$ref`("#/components/schemas/Inner") }
        val (openApi, config) = specWithBinding(parent)
        config.groupedSchemaCustomizer().customise(openApi)
        assertCloneRewroteRefIn(openApi) { it.contains }
    }

    @Test
    fun `clone rewrites $ref inside if`() {
        val parent = ObjectSchema().apply { `if` = Schema<Any>().`$ref`("#/components/schemas/Inner") }
        val (openApi, config) = specWithBinding(parent)
        config.groupedSchemaCustomizer().customise(openApi)
        assertCloneRewroteRefIn(openApi) { it.`if` }
    }

    @Test
    fun `clone rewrites $ref inside then`() {
        val parent = ObjectSchema().apply { then = Schema<Any>().`$ref`("#/components/schemas/Inner") }
        val (openApi, config) = specWithBinding(parent)
        config.groupedSchemaCustomizer().customise(openApi)
        assertCloneRewroteRefIn(openApi) { it.then }
    }

    @Test
    fun `clone rewrites $ref inside else`() {
        val parent = ObjectSchema().apply { `else` = Schema<Any>().`$ref`("#/components/schemas/Inner") }
        val (openApi, config) = specWithBinding(parent)
        config.groupedSchemaCustomizer().customise(openApi)
        assertCloneRewroteRefIn(openApi) { it.`else` }
    }

    @Test
    fun `clone rewrites $ref inside dependentSchemas`() {
        val parent = ObjectSchema().apply {
            dependentSchemas = mutableMapOf("trigger" to Schema<Any>().`$ref`("#/components/schemas/Inner"))
        }
        val (openApi, config) = specWithBinding(parent)
        config.groupedSchemaCustomizer().customise(openApi)
        assertCloneRewroteRefIn(openApi) { it.dependentSchemas?.values?.first() }
    }

    @Test
    fun `clone rewrites $ref inside additionalItems`() {
        val parent = ArraySchema().apply { additionalItems = Schema<Any>().`$ref`("#/components/schemas/Inner") }
        val (openApi, config) = specWithBinding(parent)
        config.groupedSchemaCustomizer().customise(openApi)
        assertCloneRewroteRefIn(openApi) { it.additionalItems }
    }

    @Test
    fun `clone rewrites $ref inside unevaluatedItems`() {
        val parent = ArraySchema().apply { unevaluatedItems = Schema<Any>().`$ref`("#/components/schemas/Inner") }
        val (openApi, config) = specWithBinding(parent)
        config.groupedSchemaCustomizer().customise(openApi)
        assertCloneRewroteRefIn(openApi) { it.unevaluatedItems }
    }

    @Test
    fun `clone preserves V31 plural examples on a property`() {
        val parent = ObjectSchema().apply {
            properties = mutableMapOf(
                "name" to StringSchema().apply {
                    examples = mutableListOf("alice", "bob")
                    addExtension(
                        "x-validations",
                        mutableListOf(
                            mutableMapOf<String, Any>(
                                "rule" to "notBlank",
                                "code" to "BLANK",
                                "groups" to listOf("CreateGroup"),
                            ),
                        ),
                    )
                },
            )
        }
        val operation = Operation().requestBody(
            RequestBody().content(
                Content().addMediaType(
                    "application/json",
                    MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/Parent")),
                ),
            ),
        )
        val openApi = OpenAPI().apply {
            components = Components()
            paths = Paths().addPathItem("/v1/x", PathItem().post(operation))
        }
        openApi.components.addSchemas("Parent", parent)
        val config = newCustomizerWithBinding(operation, listOf("CreateGroup"))

        config.groupedSchemaCustomizer().customise(openApi)

        val clone = openApi.components.schemas["Parent_CreateGroup"]
            ?: error("Parent_CreateGroup missing")
        val cloneNameExamples = clone.properties?.get("name")?.examples
        assertThat(cloneNameExamples).containsExactly("alice", "bob")
    }

    @Test
    fun `clone preserves V31 const on a property`() {
        val parent = ObjectSchema().apply {
            properties = mutableMapOf(
                "kind" to StringSchema().apply {
                    setConst("ACTIVE")
                    addExtension(
                        "x-validations",
                        mutableListOf(
                            mutableMapOf<String, Any>(
                                "rule" to "notBlank",
                                "code" to "BLANK",
                                "groups" to listOf("CreateGroup"),
                            ),
                        ),
                    )
                },
            )
        }
        val operation = Operation().requestBody(
            RequestBody().content(
                Content().addMediaType(
                    "application/json",
                    MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/Parent")),
                ),
            ),
        )
        val openApi = OpenAPI().apply {
            components = Components()
            paths = Paths().addPathItem("/v1/x", PathItem().post(operation))
        }
        openApi.components.addSchemas("Parent", parent)
        val config = newCustomizerWithBinding(operation, listOf("CreateGroup"))

        config.groupedSchemaCustomizer().customise(openApi)

        val clone = openApi.components.schemas["Parent_CreateGroup"]
            ?: error("Parent_CreateGroup missing")
        assertThat(clone.properties?.get("kind")?.getConst()).isEqualTo("ACTIVE")
    }

    @Test
    fun `clone preserves dollar comment on a property`() {
        val parent = ObjectSchema().apply {
            properties = mutableMapOf(
                "name" to StringSchema().apply {
                    `set$comment`("design-note: never empty")
                    addExtension(
                        "x-validations",
                        mutableListOf(
                            mutableMapOf<String, Any>(
                                "rule" to "notBlank",
                                "code" to "BLANK",
                                "groups" to listOf("CreateGroup"),
                            ),
                        ),
                    )
                },
            )
        }
        val operation = Operation().requestBody(
            RequestBody().content(
                Content().addMediaType(
                    "application/json",
                    MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/Parent")),
                ),
            ),
        )
        val openApi = OpenAPI().apply {
            components = Components()
            paths = Paths().addPathItem("/v1/x", PathItem().post(operation))
        }
        openApi.components.addSchemas("Parent", parent)
        val config = newCustomizerWithBinding(operation, listOf("CreateGroup"))

        config.groupedSchemaCustomizer().customise(openApi)

        val clone = openApi.components.schemas["Parent_CreateGroup"]
            ?: error("Parent_CreateGroup missing")
        assertThat(clone.properties?.get("name")?.`$comment`).isEqualTo("design-note: never empty")
    }

    @Test
    fun `clone preserves V31 booleanSchemaValue on a property`() {
        val parent = ObjectSchema().apply {
            properties = mutableMapOf(
                // A property whose schema is literally `true` (JSON Schema 2020-12 boolean
                // schema; matches anything). swagger represents this via booleanSchemaValue.
                "always" to Schema<Any>().apply { booleanSchemaValue = true },
                // Sibling property carries the group-scoped constraint that triggers cloning.
                "name" to StringSchema().apply {
                    addExtension(
                        "x-validations",
                        mutableListOf(
                            mutableMapOf<String, Any>(
                                "rule" to "notBlank",
                                "code" to "BLANK",
                                "groups" to listOf("CreateGroup"),
                            ),
                        ),
                    )
                },
            )
        }
        val operation = Operation().requestBody(
            RequestBody().content(
                Content().addMediaType(
                    "application/json",
                    MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/Parent")),
                ),
            ),
        )
        val openApi = OpenAPI().apply {
            components = Components()
            paths = Paths().addPathItem("/v1/x", PathItem().post(operation))
        }
        openApi.components.addSchemas("Parent", parent)
        val config = newCustomizerWithBinding(operation, listOf("CreateGroup"))

        config.groupedSchemaCustomizer().customise(openApi)

        val clone = openApi.components.schemas["Parent_CreateGroup"]
            ?: error("Parent_CreateGroup missing")
        assertThat(clone.properties?.get("always")?.booleanSchemaValue).isTrue()
    }

    @Test
    fun `clone preserves dependentRequired on the parent schema`() {
        val parent = ObjectSchema().apply {
            properties = mutableMapOf(
                "name" to StringSchema().apply {
                    addExtension(
                        "x-validations",
                        mutableListOf(
                            mutableMapOf<String, Any>(
                                "rule" to "notBlank",
                                "code" to "BLANK",
                                "groups" to listOf("CreateGroup"),
                            ),
                        ),
                    )
                },
                "email" to StringSchema(),
                "phone" to StringSchema(),
            )
            dependentRequired = mutableMapOf("name" to mutableListOf("email", "phone"))
        }
        val operation = Operation().requestBody(
            RequestBody().content(
                Content().addMediaType(
                    "application/json",
                    MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/Parent")),
                ),
            ),
        )
        val openApi = OpenAPI().apply {
            components = Components()
            paths = Paths().addPathItem("/v1/x", PathItem().post(operation))
        }
        openApi.components.addSchemas("Parent", parent)
        val config = newCustomizerWithBinding(operation, listOf("CreateGroup"))

        config.groupedSchemaCustomizer().customise(openApi)

        val clone = openApi.components.schemas["Parent_CreateGroup"]
            ?: error("Parent_CreateGroup missing")
        assertThat(clone.dependentRequired).isEqualTo(mapOf("name" to listOf("email", "phone")))
    }

    @Test
    fun `cloning a V31 schema-typed field leaves the original untouched`() {
        val parent = ObjectSchema().apply {
            patternProperties = mutableMapOf("^x-.*$" to Schema<Any>().`$ref`("#/components/schemas/Inner"))
        }
        val (openApi, config) = specWithBinding(parent)
        // Add a second operation that references Parent without a group pin so the
        // original Parent component survives the orphan-pruning step.
        val secondOperation = Operation().requestBody(
            RequestBody().content(
                Content().addMediaType(
                    "application/json",
                    MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/Parent")),
                ),
            ),
        )
        openApi.paths.addPathItem("/v1/y", PathItem().post(secondOperation))

        config.groupedSchemaCustomizer().customise(openApi)

        val originalParent = openApi.components.schemas["Parent"]
            ?: error("Original Parent missing")
        val originalInner = originalParent.patternProperties?.values?.first()
            ?: error("Original Parent.patternProperties missing")
        assertThat(originalInner.`$ref`)
            .`as`("Cloning must not mutate the original component's V31 sub-schema reference")
            .isEqualTo("#/components/schemas/Inner")
    }
}
