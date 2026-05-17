package group.phorus.exception.bdd

import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Validated(CreateGroup::class)
@RestController
class TestClassLevelValidatedController {

    @PostMapping("/v1/testClassLevelValidatedCreate")
    fun testClassLevelValidatedCreate(@RequestBody body: GroupsDto): String = "OK"
}
