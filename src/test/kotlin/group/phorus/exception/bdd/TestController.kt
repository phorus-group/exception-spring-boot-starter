package group.phorus.exception.bdd

import group.phorus.exception.core.*
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class TestController {

    @PostMapping("/v1/testException")
    fun testException(@RequestBody exception: String): String {
        when (exception) {
            "BadRequest" -> throw BadRequest("Bad request")
            "NotFound" -> throw NotFound("Not found")
            "Conflict" -> throw Conflict("Conflict")
            "Unauthorized" -> throw Unauthorized("Unauthorized")
            "Forbidden" -> throw Forbidden("Forbidden")
            "RequestTimeout" -> throw RequestTimeout("Request timeout")
            "InternalServerError" -> throw InternalServerError("Internal server error")
            "MethodNotAllowed" -> throw MethodNotAllowed("Method not allowed")
            "TooManyRequests" -> throw TooManyRequests("Too many requests")
            "ServiceUnavailable" -> throw ServiceUnavailable("Service unavailable")
            "BadGateway" -> throw BadGateway("Bad gateway")
            "GatewayTimeout" -> throw GatewayTimeout("Gateway timeout")
            "UnprocessableEntity" -> throw UnprocessableEntity("Unprocessable entity")
            "Gone" -> throw Gone("Gone")
            "PreconditionFailed" -> throw PreconditionFailed("Precondition failed")
            "UnsupportedMediaType" -> throw UnsupportedMediaType("Unsupported media type")
        }
        return "OK"
    }

    @PostMapping("/v1/testExceptionWithCode")
    fun testExceptionWithCode(@RequestBody body: Map<String, String>): String {
        throw BadRequest(body["message"], code = body["code"])
    }

    @PostMapping("/v1/testExceptionWithMetadata")
    fun testExceptionWithMetadata(@RequestBody body: Map<String, String>): String {
        throw BadRequest(
            body["message"],
            code = body["code"],
            metadata = mapOf("field" to "email", "limit" to 255),
        )
    }

    @PostMapping("/v1/testFail")
    fun testFail(): String {
        throw RuntimeException("unexpected")
    }

    @PostMapping("/v1/test")
    fun test(@Valid @RequestBody testObject: TestObject): String {
        return "OK"
    }

    @PostMapping("/v1/testList")
    fun testList(@RequestBody @Valid testObject: List<@NotNull TestObject>): String {
        return "OK"
    }
}
