package group.phorus.exception.bdd.steps

import group.phorus.exception.bdd.RequestScenarioScope
import group.phorus.exception.bdd.ResponseScenarioScope
import group.phorus.exception.bdd.TestObject
import group.phorus.exception.bdd.TestSubObject
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient


class BaseStepsDefinition(
    @Autowired private val webTestClient: WebTestClient,
    @Autowired private val requestScenarioScope: RequestScenarioScope,
    @Autowired private val responseScenarioScope: ResponseScenarioScope,
    @Autowired private val meterRegistry: MeterRegistry,
) {

    @Given("^the caller has an object that will result in a (BadRequest|NotFound|Conflict|Unauthorized|Forbidden|RequestTimeout|InternalServerError|MethodNotAllowed|TooManyRequests|ServiceUnavailable|BadGateway|GatewayTimeout|UnprocessableEntity|Gone|PreconditionFailed|UnsupportedMediaType) exception$")
    fun `the caller has an object that will result in a {exception} exception`(exception: String) {
        requestScenarioScope.request = exception
    }

    @Given("the caller has an exception with code {string}")
    fun `the caller has an exception with code`(code: String) {
        requestScenarioScope.request = mapOf("message" to "Bad request", "code" to code)
    }

    @Given("the caller has an exception with metadata")
    fun `the caller has an exception with metadata`() {
        requestScenarioScope.request = mapOf("message" to "Bad request", "code" to "VALIDATION_001")
    }

    @Given("the caller has a normal object")
    fun `the caller has a normal object`() {
        requestScenarioScope.request = TestObject(
            testVar = "test",
            testInt = 1,
            subObject = TestSubObject(testVar = "test1"),
            subObjectList = listOf(TestSubObject(testVar = "test2")),
        )
    }

    @Given("the caller has a list containing an object with null field that cannot be null")
    fun `the caller has a list containing an object with null field that cannot be null`() {
        requestScenarioScope.request = listOf(
            TestObject(
                testVar = "test",
                testInt = null,
                subObject = TestSubObject(testVar = "test1"),
                subObjectList = listOf(TestSubObject(testVar = "test2")),
            )
        )
    }

    @Given("the caller has a null field that cannot be null")
    fun `the caller has a null field that cannot be null`() {
        requestScenarioScope.request = TestObject(
            testVar = "test",
            testInt = null,
            subObject = TestSubObject(testVar = "test1"),
            subObjectList = listOf(TestSubObject(testVar = "test2")),
        )
    }

    @Given("the caller has a blank field that cannot be blank")
    fun `the caller has a blank field that cannot be blank`() {
        requestScenarioScope.request = TestObject(
            testVar = "",
            testInt = 1,
            subObject = TestSubObject(testVar = "test1"),
            subObjectList = listOf(TestSubObject(testVar = "test2")),
        )
    }

    @Given("the caller has a null field that cannot be blank")
    fun `the caller has a null field that cannot be blank`() {
        requestScenarioScope.request = TestObject(
            testVar = null,
            testInt = 1,
            subObject = TestSubObject(testVar = "test1"),
            subObjectList = listOf(TestSubObject(testVar = "test2")),
        )
    }

    @Given("the caller has an empty list field that cannot be empty")
    fun `the caller has an empty list field that cannot be empty`() {
        requestScenarioScope.request = TestObject(
            testVar = "test",
            testInt = 1,
            subObject = TestSubObject(testVar = "test1"),
            subObjectList = emptyList(),
        )
    }

    @Given("the caller has a null list field that cannot be null")
    fun `the caller has a null list field that cannot be null`() {
        requestScenarioScope.request = TestObject(
            testVar = "test",
            testInt = 1,
            subObject = TestSubObject(testVar = "test1"),
            subObjectList = null,
        )
    }

    @Given("the caller has a blank subfield that cannot be blank")
    fun `the caller has a blank subfield that cannot be blank`() {
        requestScenarioScope.request = TestObject(
            testVar = "test",
            testInt = 1,
            subObject = TestSubObject(testVar = ""),
            subObjectList = listOf(TestSubObject(testVar = "test2")),
        )
    }


    @When("the external service calls the {string} endpoint")
    fun `when the external service calls the {string} endpoint`(endpoint: String) {
        webTestClient.post()
            .uri { it.path(endpoint).build() }
        val spec = webTestClient.post()
            .uri { it.path(endpoint).build() }
        val request = requestScenarioScope.request
        val exchangeSpec = if (request != null) spec.bodyValue(request).exchange() else spec.exchange()
        responseScenarioScope.responseSpec = exchangeSpec
    }


    @Then("the service returns HTTP {int}")
    fun `and the service returns HTTP code`(httpCode: Int) {
        responseScenarioScope.responseSpec!!
            .expectStatus().isEqualTo(httpCode)
    }

    @Then("the response has status {int} and title {string}")
    fun `the response has status and title`(status: Int, title: String) {
        responseScenarioScope.responseSpec!!
            .expectBody()
            .jsonPath("$.status").isEqualTo(status)
            .jsonPath("$.title").isEqualTo(title)
    }

    @Then("the response contains code {string}")
    fun `the response contains code`(code: String) {
        responseScenarioScope.responseSpec!!
            .expectBody()
            .jsonPath("$.code").isEqualTo(code)
    }

    @Then("the response does not contain code")
    fun `the response does not contain code`() {
        responseScenarioScope.responseSpec!!
            .expectBody()
            .jsonPath("$.code").doesNotExist()
    }

    @Then("the response contains source {string}")
    fun `the response contains source`(source: String) {
        responseScenarioScope.responseSpec!!
            .expectBody()
            .jsonPath("$.source").isEqualTo(source)
    }

    @Then("the response contains metadata field {string} with value {string}")
    fun `the response contains metadata field with string value`(field: String, value: String) {
        responseScenarioScope.responseSpec!!
            .expectBody()
            .jsonPath("$.metadata.$field").isEqualTo(value)
    }

    @Then("the response contains metadata field {string} with value {int}")
    fun `the response contains metadata field with int value`(field: String, value: Int) {
        responseScenarioScope.responseSpec!!
            .expectBody()
            .jsonPath("$.metadata.$field").isEqualTo(value)
    }

    @Then("the service returns a message with the validation errors")
    fun `the service returns a message with the validation errors`(data: DataTable) {
        val obj = data.asMaps().first()["obj"]!!
        val field = data.asMaps().first()["field"]!!
        val rejectedValue = data.asMaps().first()["rejectedValue"]!!
        val message = data.asMaps().first()["message"]!!

        responseScenarioScope.responseSpec!!
            .expectBody()
            .jsonPath("$.validationErrors[0].obj").isEqualTo(obj)
            .jsonPath("$.validationErrors[0].field").isEqualTo(field)
            .let {
                when (rejectedValue) {
                    "null" -> it.jsonPath("$.validationErrors[0].rejectedValue").doesNotExist()
                    "blank" -> it.jsonPath("$.validationErrors[0].rejectedValue").isEqualTo("")
                    "[]" -> it.jsonPath("$.validationErrors[0].rejectedValue").isEmpty
                    else -> it.jsonPath("$.validationErrors[0].rejectedValue").isEqualTo(rejectedValue)
                }
            }
            .jsonPath("$.validationErrors[0].message").isEqualTo(message)
    }

    @Then("the metric {string} is recorded with type {string} and status {int}")
    fun `the metric is recorded with type and status`(metricName: String, type: String, status: Int) {
        val counter = meterRegistry.find(metricName)
            .tag("type", type)
            .tag("status_code", status.toString())
            .counter()

        assertNotNull(counter, "Counter '$metricName' with type=$type, status_code=$status should exist")
        assertTrue(counter!!.count() >= 1.0,
            "Counter '$metricName' with type=$type, status_code=$status should have been incremented")
    }
}
