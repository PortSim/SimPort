package com.group7

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class Port1Tests :
    FunSpec({
        test("Everything sent into port 1 eventually comes out") {
            val numTrucks = 100

            val (scenario, sink) = generatePort(numTrucks = numTrucks)
            val simulator = Simulator(EventLog.noop(), scenario)
            while (!simulator.isFinished) {
                simulator.nextStep()
            }

            sink.reportMetrics().occupants shouldBe numTrucks
        }
    })
