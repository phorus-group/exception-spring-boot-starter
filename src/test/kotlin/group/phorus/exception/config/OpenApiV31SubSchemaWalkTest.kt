package group.phorus.exception.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.callbacks.Callback
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.links.Link
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.Discriminator
import io.swagger.v3.oas.models.media.Encoding
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Exercises the customizer's whole-document walks on V31 sub-schema positions
 * (`patternProperties`, `contentSchema`, `prefixItems`, `propertyNames`, `not`,
 * `contains`, `items`) that the property pipeline does not visit at depth 1.
 * Synthetic OpenAPI documents are constructed by hand because springdoc does
 * not natively emit these positions for typical Kotlin DTOs.
 */
class OpenApiV31SubSchemaWalkTest {

    private fun specWithRequestBodyRef(componentName: String): OpenAPI {
        val operation = Operation().requestBody(
            RequestBody().content(
                Content().addMediaType(
                    "application/json",
                    MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/$componentName")),
                ),
            ),
        )
        return OpenAPI().apply {
            components = Components()
            paths = Paths().addPathItem("/v1/x", PathItem().post(operation))
        }
    }

    private fun runCustomizer(openApi: OpenAPI) {
        OpenApiAutoConfiguration().groupedSchemaCustomizer().customise(openApi)
    }

    @Test
    fun `prune preserves a component reached only through patternProperties`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas(
            "Parent",
            ObjectSchema().apply {
                patternProperties = mutableMapOf(
                    "^x-.*$" to Schema<Any>().`$ref`("#/components/schemas/Inner"),
                )
            },
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys)
            .`as`("Inner is reachable only via patternProperties; must not be pruned")
            .contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through prefixItems`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas(
            "Parent",
            ArraySchema().apply {
                prefixItems = mutableListOf(Schema<Any>().`$ref`("#/components/schemas/Inner"))
            },
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through contentSchema`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas(
            "Parent",
            ObjectSchema().apply {
                contentSchema = Schema<Any>().`$ref`("#/components/schemas/Inner")
            },
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through propertyNames`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas(
            "Parent",
            ObjectSchema().apply {
                propertyNames = Schema<Any>().`$ref`("#/components/schemas/Inner")
            },
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through not`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas(
            "Parent",
            ObjectSchema().apply {
                not = Schema<Any>().`$ref`("#/components/schemas/Inner")
            },
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through contains`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas(
            "Parent",
            ArraySchema().apply {
                contains = Schema<Any>().`$ref`("#/components/schemas/Inner")
            },
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `strip removes internal groups key from nested items x-validations`() {
        val openApi = specWithRequestBodyRef("Parent")
        val nested = StringSchema().apply {
            addExtension(
                "x-validations",
                mutableListOf(
                    mutableMapOf<String, Any>(
                        "rule" to "notBlank",
                        "code" to "BLANK",
                        "groups" to emptyList<String>(),
                        "value" to "scratch",
                    ),
                ),
            )
        }
        openApi.components.addSchemas("Parent", ArraySchema().apply { items = nested })

        runCustomizer(openApi)

        @Suppress("UNCHECKED_CAST")
        val entries = nested.extensions["x-validations"] as List<Map<String, Any>>
        assertThat(entries).hasSize(1)
        assertThat(entries[0]).doesNotContainKey("groups")
        assertThat(entries[0]).doesNotContainKey("value")
    }

    @Test
    fun `prune preserves a component reached only through additionalItems`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas(
            "Parent",
            ArraySchema().apply { additionalItems = Schema<Any>().`$ref`("#/components/schemas/Inner") },
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through unevaluatedItems`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas(
            "Parent",
            ArraySchema().apply { unevaluatedItems = Schema<Any>().`$ref`("#/components/schemas/Inner") },
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through if`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas(
            "Parent",
            ObjectSchema().apply { `if` = Schema<Any>().`$ref`("#/components/schemas/Inner") },
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through then`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas(
            "Parent",
            ObjectSchema().apply { then = Schema<Any>().`$ref`("#/components/schemas/Inner") },
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through else`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas(
            "Parent",
            ObjectSchema().apply { `else` = Schema<Any>().`$ref`("#/components/schemas/Inner") },
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through dependentSchemas`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas(
            "Parent",
            ObjectSchema().apply {
                dependentSchemas = mutableMapOf("trigger" to Schema<Any>().`$ref`("#/components/schemas/Inner"))
            },
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through discriminator mapping with full ref`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas(
            "Parent",
            ObjectSchema().apply {
                discriminator = Discriminator().propertyName("kind").apply {
                    mapping = mutableMapOf("dog" to "#/components/schemas/Inner")
                }
            },
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through discriminator mapping with bare type name`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas(
            "Parent",
            ObjectSchema().apply {
                discriminator = Discriminator().propertyName("kind").apply {
                    mapping = mutableMapOf("dog" to "Inner")
                }
            },
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through an operation callback request body`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas("Parent", StringSchema())
        val callbackPathItem = PathItem().post(
            Operation().requestBody(
                RequestBody().content(
                    Content().addMediaType(
                        "application/json",
                        MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/Inner")),
                    ),
                ),
            ),
        )
        openApi.paths.values.first().post.callbacks = mutableMapOf(
            "onCreate" to Callback().addPathItem("{\$request.body#/url}", callbackPathItem),
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through a response header schema`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas("Parent", StringSchema())
        val response = ApiResponse().description("ok").apply {
            headers = mutableMapOf(
                "X-Trace" to Header().schema(Schema<Any>().`$ref`("#/components/schemas/Inner")),
            )
        }
        openApi.paths.values.first().post.responses = ApiResponses().addApiResponse("200", response)
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through a response header content`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas("Parent", StringSchema())
        val response = ApiResponse().description("ok").apply {
            headers = mutableMapOf(
                "X-Trace" to Header().content(
                    Content().addMediaType(
                        "application/json",
                        MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/Inner")),
                    ),
                ),
            )
        }
        openApi.paths.values.first().post.responses = ApiResponses().addApiResponse("200", response)
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through a parameter content`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas("Parent", StringSchema())
        openApi.paths.values.first().post.parameters = mutableListOf(
            Parameter().name("filter").`in`("query").content(
                Content().addMediaType(
                    "application/json",
                    MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/Inner")),
                ),
            ),
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through encoding headers`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas("Parent", StringSchema())
        val mediaType = openApi.paths.values.first().post.requestBody.content["application/json"]!!
        mediaType.encoding = mutableMapOf(
            "file" to Encoding().headers(
                mutableMapOf(
                    "X-Checksum" to Header().schema(Schema<Any>().`$ref`("#/components/schemas/Inner")),
                ),
            ),
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through a webhook request body`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas("Parent", StringSchema())
        openApi.webhooks = mutableMapOf(
            "newPet" to PathItem().post(
                Operation().requestBody(
                    RequestBody().content(
                        Content().addMediaType(
                            "application/json",
                            MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/Inner")),
                        ),
                    ),
                ),
            ),
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through a path-level parameter`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas("Parent", StringSchema())
        openApi.paths.values.first().parameters = mutableListOf(
            Parameter().name("tenant").`in`("header")
                .schema(Schema<Any>().`$ref`("#/components/schemas/Inner")),
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through a components dot parameters entry`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas("Parent", StringSchema())
        openApi.components.parameters = mutableMapOf(
            "Tenant" to Parameter().name("tenant").`in`("header")
                .schema(Schema<Any>().`$ref`("#/components/schemas/Inner")),
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through a components dot responses entry`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas("Parent", StringSchema())
        openApi.components.responses = mutableMapOf(
            "Failure" to ApiResponse().description("fail").content(
                Content().addMediaType(
                    "application/json",
                    MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/Inner")),
                ),
            ),
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through a components dot requestBodies entry`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas("Parent", StringSchema())
        openApi.components.requestBodies = mutableMapOf(
            "Shared" to RequestBody().content(
                Content().addMediaType(
                    "application/json",
                    MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/Inner")),
                ),
            ),
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through a components dot headers entry`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas("Parent", StringSchema())
        openApi.components.headers = mutableMapOf(
            "Trace" to Header().schema(Schema<Any>().`$ref`("#/components/schemas/Inner")),
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through a components dot callbacks entry`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas("Parent", StringSchema())
        openApi.components.callbacks = mutableMapOf(
            "OnEvent" to Callback().addPathItem(
                "{\$request.body#/url}",
                PathItem().post(
                    Operation().requestBody(
                        RequestBody().content(
                            Content().addMediaType(
                                "application/json",
                                MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/Inner")),
                            ),
                        ),
                    ),
                ),
            ),
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `prune preserves a component reached only through a components dot pathItems entry`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas("Parent", StringSchema())
        openApi.components.pathItems = mutableMapOf(
            "Shared" to PathItem().post(
                Operation().requestBody(
                    RequestBody().content(
                        Content().addMediaType(
                            "application/json",
                            MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/Inner")),
                        ),
                    ),
                ),
            ),
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    @Suppress("DEPRECATION")
    fun `prune preserves a component reached only through a response link header schema`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas("Parent", StringSchema())
        val link = Link().apply {
            headers = mutableMapOf(
                "X-Trace" to Header().schema(Schema<Any>().`$ref`("#/components/schemas/Inner")),
            )
        }
        val response = ApiResponse().description("ok").apply {
            links = mutableMapOf("self" to link)
        }
        openApi.paths.values.first().post.responses = ApiResponses().addApiResponse("200", response)
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    @Suppress("DEPRECATION")
    fun `prune preserves a component reached only through a components dot links entry header schema`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas("Parent", StringSchema())
        openApi.components.links = mutableMapOf(
            "Shared" to Link().apply {
                headers = mutableMapOf(
                    "X-Trace" to Header().schema(Schema<Any>().`$ref`("#/components/schemas/Inner")),
                )
            },
        )
        openApi.components.addSchemas("Inner", StringSchema())

        runCustomizer(openApi)

        assertThat(openApi.components.schemas.keys).contains("Inner")
    }

    @Test
    fun `strip removes internal groups key from a response header x-validations`() {
        val openApi = specWithRequestBodyRef("Parent")
        openApi.components.addSchemas("Parent", StringSchema())
        val headerSchema = StringSchema().apply {
            addExtension(
                "x-validations",
                mutableListOf(
                    mutableMapOf<String, Any>(
                        "rule" to "notBlank",
                        "code" to "BLANK",
                        "groups" to emptyList<String>(),
                    ),
                ),
            )
        }
        val response = ApiResponse().description("ok").apply {
            headers = mutableMapOf("X-Trace" to Header().schema(headerSchema))
        }
        openApi.paths.values.first().post.responses = ApiResponses().addApiResponse("200", response)

        runCustomizer(openApi)

        @Suppress("UNCHECKED_CAST")
        val entries = headerSchema.extensions["x-validations"] as List<Map<String, Any>>
        assertThat(entries[0]).doesNotContainKey("groups")
    }

    @Test
    fun `strip removes internal groups key from nested patternProperties x-validations`() {
        val openApi = specWithRequestBodyRef("Parent")
        val nested = StringSchema().apply {
            addExtension(
                "x-validations",
                mutableListOf(
                    mutableMapOf<String, Any>(
                        "rule" to "pattern",
                        "code" to "INVALID_FORMAT",
                        "groups" to emptyList<String>(),
                    ),
                ),
            )
        }
        openApi.components.addSchemas(
            "Parent",
            ObjectSchema().apply { patternProperties = mutableMapOf("^x-.*$" to nested) },
        )

        runCustomizer(openApi)

        @Suppress("UNCHECKED_CAST")
        val entries = nested.extensions["x-validations"] as List<Map<String, Any>>
        assertThat(entries[0]).doesNotContainKey("groups")
    }
}
