package com.group7.generators

import com.group7.utils.TestVehicle
import com.group7.utils.TestVehicle1
import com.group7.utils.TestVehicle2
import com.group7.utils.TestVehicle3
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

class GeneratorTest :
    FunSpec({
        test("Constant generator creates the same object") {
            val delay = 10.seconds
            val gen = Generators.constant(TestVehicle, delayProvider = Delays.fixed(delay))

            val result = List(10) { gen.next() }

            result shouldBe List(10) { TestVehicle to delay }
        }

        test("Alternating generator correctly cycles vehicle types") {
            val delay = 10.seconds
            val gen =
                Generators.alternating(TestVehicle1, TestVehicle2, TestVehicle3, delayProvider = Delays.fixed(delay))

            val result = List(10) { gen.next() }

            result shouldBe
                listOf(
                    TestVehicle1 to delay,
                    TestVehicle2 to delay,
                    TestVehicle3 to delay,
                    TestVehicle1 to delay,
                    TestVehicle2 to delay,
                    TestVehicle3 to delay,
                    TestVehicle1 to delay,
                    TestVehicle2 to delay,
                    TestVehicle3 to delay,
                    TestVehicle1 to delay,
                )
        }
    })
