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
