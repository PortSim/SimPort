import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ExampleTest : FunSpec({

    test("1 + 2 should equal 3") {
        val result = 1 + 2
        result shouldBe 3
    }
})