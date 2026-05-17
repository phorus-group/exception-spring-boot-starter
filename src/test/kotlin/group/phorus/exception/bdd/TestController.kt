package group.phorus.exception.bdd

import group.phorus.exception.core.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
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

    @PostMapping("/v1/testGroupsCreate")
    fun testGroupsCreate(
        @RequestBody @Validated(CreateGroup::class) body: GroupsDto,
    ): String = "OK"

    @PostMapping("/v1/testGroupsUpdate")
    fun testGroupsUpdate(
        @RequestBody @Validated(UpdateGroup::class) body: GroupsDto,
    ): String = "OK"

    @PostMapping("/v1/testUngroupedA")
    fun testUngroupedA(@RequestBody @Valid body: UngroupedDto): String = "OK"

    @PostMapping("/v1/testUngroupedB")
    fun testUngroupedB(@RequestBody @Valid body: UngroupedDto): String = "OK"

    // Method-level @Validated: the group pin lives on the method, not on the
    // @RequestBody parameter. Spring 6.1+ honors this at runtime; the openapi
    // customizer must read it from the same place.

    @PostMapping("/v1/testGroupsCreateMethodLevel")
    @Validated(CreateGroup::class)
    fun testGroupsCreateMethodLevel(@RequestBody body: GroupsDto): String = "OK"

    @PostMapping("/v1/testGroupsUpdateMethodLevel")
    @Validated(UpdateGroup::class)
    fun testGroupsUpdateMethodLevel(@RequestBody body: GroupsDto): String = "OK"

    // HandlerMethodValidationException path (Spring 6.1+): constraint annotations
    // directly on @RequestParam / @PathVariable raise this exception.

    @PostMapping("/v1/testParamMin")
    fun testParamMin(@RequestParam @Min(5) limit: Int): String = "OK"

    // Parameter-level constraints on @PathVariable / @RequestParam / @RequestHeader /
    // @CookieValue. The ParameterCustomizer must attach x-validations on the parameter's
    // schema using the same {rule, code} mapping the body PropertyCustomizer uses.

    @PostMapping("/v1/testPathParam/{id}")
    fun testPathParam(
        @PathVariable @NotBlank @Size(max = 10) id: String,
    ): String = "OK"

    @PostMapping("/v1/testHeaderParam")
    fun testHeaderParam(
        @RequestHeader("X-Trace") @NotBlank traceId: String,
    ): String = "OK"

    @PostMapping("/v1/testCookieParam")
    fun testCookieParam(
        @CookieValue @NotBlank session: String,
    ): String = "OK"

    @PostMapping("/v1/testParamPattern")
    fun testParamPattern(
        @RequestParam @Pattern(regexp = "[A-Z]+") code: String,
    ): String = "OK"

    @PostMapping("/v1/testParamPlain")
    fun testParamPlain(@RequestParam name: String): String = "OK"

    @PostMapping("/v1/testValidOnGroupedDto")
    fun testValidOnGroupedDto(@RequestBody @Valid body: GroupsDto): String = "OK"

    @PostMapping("/v1/testValidatedNoGroupOnGroupedDto")
    fun testValidatedNoGroupOnGroupedDto(@RequestBody @Validated body: GroupsDto): String = "OK"

    @PostMapping("/v1/testValidOnNestedGroups")
    fun testValidOnNestedGroups(@RequestBody @Valid body: OuterGroupsDto): String = "OK"

    @PostMapping("/v1/testValidOnMixed")
    fun testValidOnMixed(@RequestBody @Valid body: MixedConstraintsDto): String = "OK"

    @PostMapping("/v1/testOnlyGroupsOrphan")
    fun testOnlyGroupsOrphan(
        @RequestBody @Validated(CreateGroup::class) body: OnlyGroupsOrphanDto,
    ): String = "OK"

    @PostMapping("/v1/testCollectionGroupsCreate")
    fun testCollectionGroupsCreate(
        @RequestBody @Validated(CreateGroup::class) body: CollectionGroupsDto,
    ): String = "OK"

    @PostMapping("/v1/testGroupedValidatorsCreate")
    fun testGroupedValidatorsCreate(
        @RequestBody @Validated(CreateGroup::class) body: GroupedValidatorsDto,
    ): String = "OK"

    @PostMapping("/v1/testGroupedValidatorsValid")
    fun testGroupedValidatorsValid(@RequestBody @Valid body: GroupedValidatorsDto): String = "OK"

    // Nested cascade: outer DTO has @field:Valid val inner. Inner has its own
    // group-scoped constraints. Endpoints pin different groups so each
    // operation's body schema must reference a per-group clone of the inner DTO
    // too, not the unscoped original.

    @PostMapping("/v1/testNestedGroupsCreate")
    fun testNestedGroupsCreate(
        @RequestBody @Validated(CreateGroup::class) body: OuterGroupsDto,
    ): String = "OK"

    @PostMapping("/v1/testNestedGroupsUpdate")
    fun testNestedGroupsUpdate(
        @RequestBody @Validated(UpdateGroup::class) body: OuterGroupsDto,
    ): String = "OK"

    @PostMapping("/v1/testMultipleGroups")
    fun testMultipleGroups(
        @RequestBody @Validated(CreateGroup::class, UpdateGroup::class) body: GroupsDto,
    ): String = "OK"

    @PostMapping("/v1/testThreeLevel")
    fun testThreeLevel(
        @RequestBody @Validated(CreateGroup::class) body: Level1Dto,
    ): String = "OK"

    @PostMapping("/v1/testOuterPlainInner")
    fun testOuterPlainInner(
        @RequestBody @Validated(CreateGroup::class) body: OuterWithPlainInnerDto,
    ): String = "OK"

    @PostMapping("/v1/testParamDedup")
    fun testParamDedup(
        @RequestParam @jakarta.validation.constraints.PositiveOrZero @Min(0) limit: Int,
    ): String = "OK"

    @PostMapping("/v1/testResponseOnly")
    fun testResponseOnly(): ResponseOnlyDto = ResponseOnlyDto(name = "hi")
}
