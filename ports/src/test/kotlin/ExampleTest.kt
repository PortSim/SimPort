package com.group7

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ExampleTest :
    FunSpec({
        test("example test that always succeeds") {
            val x = 1
            val y = 2

            (x + y) shouldBe 3
        }
    })
