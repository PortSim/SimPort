package com.group7

import com.group7.dsl.arrivals
import com.group7.dsl.buildScenario
import com.group7.dsl.thenDelay
import com.group7.dsl.thenSink
import com.group7.generators.Delays
import com.group7.generators.Generators
import com.group7.generators.take
import com.group7.nodes.SinkNode
import com.group7.utils.NUM_VEHICLES
import com.group7.utils.TestVehicle
import com.group7.utils.runSimulation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DelayNodeTest :
    FunSpec({
        test("Everything sent into a delay node eventually comes out") {
            val numCars = NUM_VEHICLES

            val sink: SinkNode<TestVehicle>
            val scenario = buildScenario {
                arrivals("Source", Generators.constant(TestVehicle, Delays.fixed(10.seconds)).take(numCars))
                    .thenDelay("Road", Delays.fixed(1.minutes))
                    .thenSink("Sink")
                    .let { sink = it }
            }

            runSimulation(scenario)

            sink.reportMetrics().occupants shouldBe numCars
        }
    })
