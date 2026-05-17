package group.phorus.exception.bdd.steps

import group.phorus.exception.bdd.ResponseScenarioScope
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

class OpenApiSteps(
    @Autowired private val webTestClient: WebTestClient,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val responseScenarioScope: ResponseScenarioScope,
) {

    @When("the caller fetches the OpenAPI document")
    fun `the caller fetches the OpenAPI document`() {
        val spec = webTestClient.get()
            .uri("/v3/api-docs")
            .exchange()
        responseScenarioScope.responseSpec = spec
    }

    @Then("the OpenAPI schema {string} has property {string} with x-validations")
    fun `schema has property with x-validations`(
        schemaName: String,
        propertyName: String,
        expected: DataTable,
    ) {
        val property = readProperty(schemaName, propertyName)
        val extension = property.get("x-validations")
            ?: error("x-validations missing on $schemaName.$propertyName")
        assertTrue(extension.isArray, "x-validations should be an array")

        val expectedRows = expected.asMaps().map { it["rule"] to it["code"] }.toSet()
        val actualRows = (0 until extension.size()).map {
            val entry = extension.get(it)
            entry.get("rule")?.asString() to entry.get("code")?.asString()
        }.toSet()

        assertEquals(expectedRows, actualRows,
            "x-validations on $schemaName.$propertyName mismatch")
    }

    @Then("the OpenAPI schema {string} has property {string} without an x-validations extension")
    fun `schema has property without x-validations`(
        schemaName: String,
        propertyName: String,
    ) {
        val property = readProperty(schemaName, propertyName)
        assertNull(property.get("x-validations"),
            "x-validations should not be present on $schemaName.$propertyName")
    }

    @Then("the OpenAPI schema {string} has property {string} with JSON Schema validators")
    fun `schema property has JSON Schema validators`(
        schemaName: String,
        propertyName: String,
        expected: DataTable,
    ) {
        val property = readProperty(schemaName, propertyName)
        expected.asMaps().forEach { row ->
            val key = row["key"] ?: error("Missing 'key' column")
            val expectedJson = row["value"] ?: error("Missing 'value' column")
            val expectedNode = objectMapper.readTree(expectedJson)
            val actualNode = property.get(key)
                ?: error("Property $schemaName.$propertyName missing JSON Schema key '$key'. " +
                    "Property contents: $property")
            assertTrue(nodesEqual(expectedNode, actualNode),
                "Property $schemaName.$propertyName.$key mismatch. " +
                    "Expected: $expectedNode. Actual: $actualNode")
        }
    }

    @Then("the OpenAPI schema {string} has {string} in its required fields")
    fun `schema has required field`(schemaName: String, fieldName: String) {
        val schema = readSchema(schemaName)
        val required = schema.get("required")
            ?: error("Schema $schemaName has no required field")
        assertTrue(required.isArray, "required should be an array")
        val values = (0 until required.size()).map { required.get(it).asString() }
        assertTrue(values.contains(fieldName),
            "Schema $schemaName.required does not contain '$fieldName'. Found: $values")
    }

    @Then("the OpenAPI body schema for POST {string} has property {string} with x-validations")
    fun `body schema has property with x-validations`(
        path: String,
        propertyName: String,
        expected: DataTable,
    ) {
        val schema = readBodySchemaForPost(path)
        val property = readProperty(schema, propertyName)
        val extension = property.get("x-validations")
            ?: error("x-validations missing on body schema for POST $path / $propertyName")
        assertTrue(extension.isArray, "x-validations should be an array")
        val expectedRows = expected.asMaps().map { it["rule"] to it["code"] }.toSet()
        val actualRows = (0 until extension.size()).map {
            val entry = extension.get(it)
            entry.get("rule")?.asString() to entry.get("code")?.asString()
        }.toSet()
        assertEquals(expectedRows, actualRows,
            "x-validations on body schema for POST $path / $propertyName mismatch")
    }

    @Then("the OpenAPI body schema for POST {string} has property {string} without an x-validations extension")
    fun `body schema has property without x-validations`(path: String, propertyName: String) {
        val schema = readBodySchemaForPost(path)
        val property = readProperty(schema, propertyName)
        assertNull(property.get("x-validations"),
            "x-validations should not be present on body schema for POST $path / $propertyName")
    }

    @Then("the OpenAPI body schema for POST {string} has property {string} without x-validations rule {string}")
    fun `body schema property has no rule`(
        path: String,
        propertyName: String,
        rule: String,
    ) {
        val schema = readBodySchemaForPost(path)
        val property = readProperty(schema, propertyName)
        val extension = property.get("x-validations") ?: return
        val rules = (0 until extension.size()).mapNotNull { extension.get(it).get("rule")?.asString() }
        assertFalse(rules.contains(rule),
            "x-validations on body schema for POST $path / $propertyName should not include rule '$rule'. Found: $rules")
    }

    @Then("the OpenAPI body schema for POST {string} has {string} in its required fields")
    fun `body schema has required field`(path: String, fieldName: String) {
        val schema = readBodySchemaForPost(path)
        val required = schema.get("required")
            ?: error("Body schema for POST $path has no required field")
        assertTrue(required.isArray, "required should be an array")
        val values = (0 until required.size()).map { required.get(it).asString() }
        assertTrue(values.contains(fieldName),
            "Body schema for POST $path required does not contain '$fieldName'. Found: $values")
    }

    @Then("the OpenAPI body schema for POST {string} does not have {string} in its required fields")
    fun `body schema does not have required field`(path: String, fieldName: String) {
        val schema = readBodySchemaForPost(path)
        val required = schema.get("required")
        if (required == null || !required.isArray) return
        val values = (0 until required.size()).map { required.get(it).asString() }
        assertFalse(values.contains(fieldName),
            "Body schema for POST $path required unexpectedly contains '$fieldName'. Found: $values")
    }

    @Then("the OpenAPI nested body schema for POST {string} property {string} has property {string} with x-validations")
    fun `nested body schema has property with x-validations`(
        path: String,
        nestedPropertyName: String,
        propertyName: String,
        expected: DataTable,
    ) {
        val nested = readNestedBodySchema(path, nestedPropertyName)
        val property = readProperty(nested, propertyName)
        val extension = property.get("x-validations")
            ?: error("x-validations missing on nested body schema for POST $path.$nestedPropertyName / $propertyName")
        assertTrue(extension.isArray, "x-validations should be an array")
        val expectedRows = expected.asMaps().map { it["rule"] to it["code"] }.toSet()
        val actualRows = (0 until extension.size()).map {
            val entry = extension.get(it)
            entry.get("rule")?.asString() to entry.get("code")?.asString()
        }.toSet()
        assertEquals(expectedRows, actualRows,
            "x-validations on nested body schema for POST $path.$nestedPropertyName / $propertyName mismatch")
    }

    @Then("the OpenAPI nested body schema for POST {string} property {string} has {string} in its required fields")
    fun `nested body schema has required field`(path: String, nestedPropertyName: String, fieldName: String) {
        val nested = readNestedBodySchema(path, nestedPropertyName)
        val required = nested.get("required")
            ?: error("Nested body schema for POST $path.$nestedPropertyName has no required field")
        assertTrue(required.isArray, "required should be an array")
        val values = (0 until required.size()).map { required.get(it).asString() }
        assertTrue(values.contains(fieldName),
            "Nested body schema for POST $path.$nestedPropertyName required does not contain '$fieldName'. Found: $values")
    }

    @Then("the OpenAPI nested body schema for POST {string} property {string} does not have {string} in its required fields")
    fun `nested body schema does not have required field`(path: String, nestedPropertyName: String, fieldName: String) {
        val nested = readNestedBodySchema(path, nestedPropertyName)
        val required = nested.get("required")
        if (required == null || !required.isArray) return
        val values = (0 until required.size()).map { required.get(it).asString() }
        assertFalse(values.contains(fieldName),
            "Nested body schema for POST $path.$nestedPropertyName required unexpectedly contains '$fieldName'. Found: $values")
    }

    @Then("the OpenAPI parameter {string} for POST {string} has x-validations")
    fun `parameter has x-validations`(
        parameterName: String,
        path: String,
        expected: DataTable,
    ) {
        val schema = readParameterSchema(path, parameterName)
        val extension = schema.get("x-validations")
            ?: error("x-validations missing on parameter $parameterName of POST $path. Schema: $schema")
        assertTrue(extension.isArray, "x-validations should be an array")
        val expectedRows = expected.asMaps().map { it["rule"] to it["code"] }.toSet()
        val actualRows = (0 until extension.size()).map {
            val entry = extension.get(it)
            entry.get("rule")?.asString() to entry.get("code")?.asString()
        }.toSet()
        assertEquals(expectedRows, actualRows,
            "x-validations on parameter $parameterName of POST $path mismatch")
    }

    @Then("the OpenAPI parameter {string} for POST {string} has no x-validations")
    fun `parameter has no x-validations`(parameterName: String, path: String) {
        val schema = readParameterSchema(path, parameterName)
        assertNull(schema.get("x-validations"),
            "x-validations should not be present on parameter $parameterName of POST $path")
    }

    @Then("the OpenAPI document declares schema {string}")
    fun `document declares schema`(schemaName: String) {
        readSchema(schemaName)
    }

    @Then("the OpenAPI document does not declare schema {string}")
    fun `document does not declare schema`(schemaName: String) {
        val schemas = readOpenApiRoot().get("components")?.get("schemas")
        val present = schemas?.get(schemaName) != null
        assertFalse(present, "Schema $schemaName should not be declared in components.schemas")
    }

    @Then("the OpenAPI schema {string} property {string} references {string}")
    fun `schema property references`(schemaName: String, propertyName: String, targetComponent: String) {
        val property = readProperty(schemaName, propertyName)
        val ref = property.get("\$ref")?.asString()
            ?: error("Property $propertyName on schema $schemaName has no \$ref")
        val actual = ref.removePrefix("#/components/schemas/")
        assertEquals(targetComponent, actual,
            "Property $schemaName.$propertyName \$ref mismatch")
    }

    @Then("the OpenAPI schema {string} items property {string} references {string}")
    fun `schema items property references`(schemaName: String, propertyName: String, targetComponent: String) {
        val property = readProperty(schemaName, propertyName)
        val items = property.get("items")
            ?: error("Property $propertyName on schema $schemaName has no items")
        val ref = items.get("\$ref")?.asString()
            ?: error("Property $propertyName.items on schema $schemaName has no \$ref")
        val actual = ref.removePrefix("#/components/schemas/")
        assertEquals(targetComponent, actual,
            "Property $schemaName.$propertyName.items \$ref mismatch")
    }

    @Then("the OpenAPI schema {string} has property {string} without JSON Schema validators")
    fun `schema property has no JSON Schema validators`(
        schemaName: String,
        propertyName: String,
        forbidden: DataTable,
    ) {
        val property = readProperty(schemaName, propertyName)
        forbidden.asMaps().forEach { row ->
            val key = row["key"] ?: error("Missing 'key' column")
            assertNull(property.get(key),
                "Property $schemaName.$propertyName should not carry JSON Schema key '$key'. Actual: ${property.get(key)}")
        }
    }

    @Then("the OpenAPI parameter {string} for POST {string} x-validations entries carry no internal keys")
    fun `parameter x-validations carry no internal keys`(parameterName: String, path: String) {
        val schema = readParameterSchema(path, parameterName)
        val extension = schema.get("x-validations")
            ?: error("x-validations missing on parameter $parameterName of POST $path")
        assertTrue(extension.isArray, "x-validations should be an array")
        for (i in 0 until extension.size()) {
            val entry = extension.get(i)
            val keys = entry.propertyNames().asSequence().toList()
            assertFalse(keys.contains("groups"),
                "x-validations entry on parameter $parameterName of POST $path leaks internal 'groups' key. Keys: $keys")
        }
    }

    private fun readProperty(schemaName: String, propertyName: String): JsonNode {
        val schema = readSchema(schemaName)
        return readProperty(schema, propertyName)
    }

    private fun readProperty(schema: JsonNode, propertyName: String): JsonNode {
        val properties = schema.get("properties")
            ?: throw IllegalStateException("Schema has no properties")
        val property = properties.get(propertyName)
            ?: throw IllegalStateException("Property $propertyName missing on schema")
        assertTrue(property.isObject, "Property $propertyName should be an object node")
        return property
    }

    private fun readParameterSchema(path: String, parameterName: String): JsonNode {
        val root = readOpenApiRoot()
        val pathNode = root.get("paths")?.get(path)
            ?: throw IllegalStateException("Path $path not found in OpenAPI document")
        val operation = pathNode.get("post")
            ?: throw IllegalStateException("POST operation not found at $path")
        val parameters = operation.get("parameters")
            ?: throw IllegalStateException("No parameters on POST $path")
        assertTrue(parameters.isArray, "parameters must be an array")
        for (i in 0 until parameters.size()) {
            val param = parameters.get(i)
            if (param.get("name")?.asString() == parameterName) {
                return param.get("schema")
                    ?: throw IllegalStateException("Parameter $parameterName has no schema on POST $path")
            }
        }
        throw IllegalStateException("Parameter $parameterName not found on POST $path")
    }

    private fun readNestedBodySchema(path: String, propertyName: String): JsonNode {
        val outer = readBodySchemaForPost(path)
        val property = readProperty(outer, propertyName)
        val ref = property.get("\$ref")?.asString()
            ?: error("Property $propertyName on body schema for POST $path has no \$ref")
        val componentName = ref.removePrefix("#/components/schemas/")
        return readSchema(componentName)
    }

    private fun readBodySchemaForPost(path: String): JsonNode {
        val root = readOpenApiRoot()
        val pathNode = root.get("paths")?.get(path)
            ?: throw IllegalStateException("Path $path not found in OpenAPI document")
        val operation = pathNode.get("post")
            ?: throw IllegalStateException("POST operation not found at $path")
        val schemaNode = operation.get("requestBody")
            ?.get("content")
            ?.get("application/json")
            ?.get("schema")
            ?: throw IllegalStateException("Request body schema missing on POST $path")
        val ref = schemaNode.get("\$ref")?.asString()
            ?: return schemaNode
        val componentName = ref.removePrefix("#/components/schemas/")
        return readSchema(componentName)
    }

    private fun readOpenApiRoot(): JsonNode {
        val responseBody = responseScenarioScope.responseSpec!!
            .expectStatus().isOk
            .expectBody()
            .returnResult()
            .responseBody!!
        return objectMapper.readTree(responseBody)
    }

    private fun readSchema(schemaName: String): JsonNode {
        val schemas = readOpenApiRoot().get("components")?.get("schemas")
            ?: throw IllegalStateException("OpenAPI document does not declare components.schemas")
        return schemas.get(schemaName)
            ?: throw IllegalStateException("Schema $schemaName not found")
    }

    private fun nodesEqual(expected: JsonNode, actual: JsonNode): Boolean {
        if (expected.isNumber && actual.isNumber) {
            return expected.decimalValue().compareTo(actual.decimalValue()) == 0
        }
        return expected == actual
    }
}
