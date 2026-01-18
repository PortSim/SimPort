package com.group7

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class Port1Tests :
    FunSpec({
        test("Everything sent into port 1 eventually comes out") {
            val numTrucks = 100

            val (simulator, sink) = generatePort(numTrucks = numTrucks)
            while (!simulator.isFinished) {
                simulator.nextStep()
            }

            sink.reportMetrics().occupants shouldBe numTrucks
        }
    })
