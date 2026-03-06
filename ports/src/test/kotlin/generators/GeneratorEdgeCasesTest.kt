package com.group7.generators

import com.group7.utils.TestVehicle
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

class GeneratorEdgeCasesTest :
    FunSpec({
        context("Generator.take") {
            test("take(n) limits generator to exactly n items") {
                val gen = Generators.constant({ TestVehicle }, Delays.fixed(10.seconds)).take(5)
                val result = List(5) { gen.next() }
                result.size shouldBe 5
                result.all { it == TestVehicle to 10.seconds } shouldBe true
            }

            test("take(n) throws NoSuchElementException after n items consumed") {
                val gen = Generators.constant({ TestVehicle }, Delays.fixed(10.seconds)).take(3)
                repeat(3) { gen.next() }
                shouldThrow<NoSuchElementException> { gen.next() }
            }

            test("take(0) yields no items") {
                val gen = Generators.constant({ TestVehicle }, Delays.fixed(10.seconds)).take(0)
                gen.hasNext() shouldBe false
            }

            test("take(1) yields exactly one item") {
                val gen = Generators.constant({ TestVehicle }, Delays.fixed(10.seconds)).take(1)
                gen.hasNext() shouldBe true
                gen.next() shouldBe (TestVehicle to 10.seconds)
                gen.hasNext() shouldBe false
            }
        }

        context("Delays.fixed") {
            test("fixed delay always returns the same duration") {
                val provider = Delays.fixed(5.seconds)
                val delays = List(10) { provider.nextDelay() }
                delays.all { it == 5.seconds } shouldBe true
            }
        }

        context("alternating generator with take") {
            test("alternating generator limited by take produces correct cycle prefix") {
                val gen =
                    Generators.alternating({ "A" }, { "B" }, { "C" }, delayProvider = Delays.fixed(1.seconds)).take(5)

                val result = List(5) { gen.next() }
                result shouldBe
                    listOf("A" to 1.seconds, "B" to 1.seconds, "C" to 1.seconds, "A" to 1.seconds, "B" to 1.seconds)
                gen.hasNext() shouldBe false
            }
        }
    })
