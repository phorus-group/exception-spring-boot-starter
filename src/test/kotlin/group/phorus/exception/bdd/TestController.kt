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

    @PostMapping("/v1/testNotNull")
    fun testNotNull(@RequestBody @Valid body: NotNullDto): String = "OK"

    @PostMapping("/v1/testNotEmptyString")
    fun testNotEmptyString(@RequestBody @Valid body: NotEmptyStringDto): String = "OK"

    @PostMapping("/v1/testNotBlank")
    fun testNotBlank(@RequestBody @Valid body: NotBlankDto): String = "OK"

    @PostMapping("/v1/testNull")
    fun testNull(@RequestBody @Valid body: NullDto): String = "OK"

    @PostMapping("/v1/testSize")
    fun testSize(@RequestBody @Valid body: SizeDto): String = "OK"

    @PostMapping("/v1/testLength")
    fun testLength(@RequestBody @Valid body: LengthDto): String = "OK"

    @PostMapping("/v1/testMin")
    fun testMin(@RequestBody @Valid body: MinDto): String = "OK"

    @PostMapping("/v1/testDecimalMin")
    fun testDecimalMin(@RequestBody @Valid body: DecimalMinDto): String = "OK"

    @PostMapping("/v1/testMax")
    fun testMax(@RequestBody @Valid body: MaxDto): String = "OK"

    @PostMapping("/v1/testDecimalMax")
    fun testDecimalMax(@RequestBody @Valid body: DecimalMaxDto): String = "OK"

    @PostMapping("/v1/testRange")
    fun testRange(@RequestBody @Valid body: RangeDto): String = "OK"

    @PostMapping("/v1/testPositive")
    fun testPositive(@RequestBody @Valid body: PositiveDto): String = "OK"

    @PostMapping("/v1/testPositiveOrZero")
    fun testPositiveOrZero(@RequestBody @Valid body: PositiveOrZeroDto): String = "OK"

    @PostMapping("/v1/testNegative")
    fun testNegative(@RequestBody @Valid body: NegativeDto): String = "OK"

    @PostMapping("/v1/testNegativeOrZero")
    fun testNegativeOrZero(@RequestBody @Valid body: NegativeOrZeroDto): String = "OK"

    @PostMapping("/v1/testDigits")
    fun testDigits(@RequestBody @Valid body: DigitsDto): String = "OK"

    @PostMapping("/v1/testPattern")
    fun testPattern(@RequestBody @Valid body: PatternDto): String = "OK"

    @PostMapping("/v1/testEmail")
    fun testEmail(@RequestBody @Valid body: EmailDto): String = "OK"

    @PostMapping("/v1/testPast")
    fun testPast(@RequestBody @Valid body: PastDto): String = "OK"

    @PostMapping("/v1/testPastOrPresent")
    fun testPastOrPresent(@RequestBody @Valid body: PastOrPresentDto): String = "OK"

    @PostMapping("/v1/testFuture")
    fun testFuture(@RequestBody @Valid body: FutureDto): String = "OK"

    @PostMapping("/v1/testFutureOrPresent")
    fun testFutureOrPresent(@RequestBody @Valid body: FutureOrPresentDto): String = "OK"

    @PostMapping("/v1/testAssertTrue")
    fun testAssertTrue(@RequestBody @Valid body: AssertTrueDto): String = "OK"

    @PostMapping("/v1/testAssertFalse")
    fun testAssertFalse(@RequestBody @Valid body: AssertFalseDto): String = "OK"

    @PostMapping("/v1/testKitchenSink")
    fun testKitchenSink(@RequestBody @Valid body: KitchenSinkDto): String = "OK"

    @PostMapping("/v1/testSizeList")
    fun testSizeList(@RequestBody @Valid body: SizeListDto): String = "OK"

    @PostMapping("/v1/testNotEmptyList")
    fun testNotEmptyList(@RequestBody @Valid body: NotEmptyListDto): String = "OK"

    @PostMapping("/v1/testSizeMaxOnly")
    fun testSizeMaxOnly(@RequestBody @Valid body: SizeMaxOnlyDto): String = "OK"

    @PostMapping("/v1/testRangeMaxOnly")
    fun testRangeMaxOnly(@RequestBody @Valid body: RangeMaxOnlyDto): String = "OK"

    @PostMapping("/v1/testRangeMinOnly")
    fun testRangeMinOnly(@RequestBody @Valid body: RangeMinOnlyDto): String = "OK"
}
