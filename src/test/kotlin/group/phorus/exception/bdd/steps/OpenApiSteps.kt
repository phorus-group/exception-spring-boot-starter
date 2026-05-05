package group.phorus.exception.bdd.steps

import group.phorus.exception.bdd.ResponseScenarioScope
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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

    private fun readProperty(schemaName: String, propertyName: String): JsonNode {
        val schema = readSchema(schemaName)
        val properties = schema.get("properties")
            ?: throw IllegalStateException("Schema $schemaName has no properties")
        val property = properties.get(propertyName)
            ?: throw IllegalStateException("Property $propertyName missing on schema $schemaName")
        assertTrue(property.isObject, "Property $propertyName should be an object node")
        return property
    }

    private fun readSchema(schemaName: String): JsonNode {
        val responseBody = responseScenarioScope.responseSpec!!
            .expectStatus().isOk
            .expectBody()
            .returnResult()
            .responseBody!!
        val root = objectMapper.readTree(responseBody)
        val schemas = root.get("components")?.get("schemas")
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
