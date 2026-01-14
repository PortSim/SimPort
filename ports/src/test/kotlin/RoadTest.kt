package com.group7

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

class RoadTest :
    FunSpec({
        test("Everything sent into a road eventually comes out") {
            val numTrucks = 100

            val (sourceOutput, roadInput) = newChannel<RoadObject>()
            val (roadOutput, sinkInput) = newChannel<RoadObject>()

            val source = Source("Source", sourceOutput, Generators.exponentialDelay(Truck, 1.0).take(numTrucks))
            val road = RoadNode("Road", roadInput, roadOutput, 5, 5.seconds)
            val sink = Sink("Sink", listOf(sinkInput))

            val port = Port(source, road, sink)

            val simulator = Simulator(EventLog.noop(), port)
            while (!simulator.isFinished) {
                simulator.nextStep()
            }

            sink.reportMetrics().occupants shouldBe numTrucks
        }
    })
