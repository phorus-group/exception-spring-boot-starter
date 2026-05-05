package group.phorus.exception.bdd

import io.cucumber.spring.ScenarioScope
import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.WebTestClient


@Component
@ScenarioScope
class RequestScenarioScope(
    var request: Any? = null,
)

@Component
@ScenarioScope
class ResponseScenarioScope(
    var responseSpec: WebTestClient.ResponseSpec? = null,
)

/**
 * Marker for tests that need to POST a raw JSON string instead of letting Jackson
 * serialize a Kotlin object. Used for negative tests that need control over wire-level
 * shape (`null` instead of missing field, raw numbers as strings, etc.).
 */
data class RawJsonPayload(val json: String)
