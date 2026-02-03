package com.group7

import com.group7.dsl.arrivals
import com.group7.dsl.buildScenario
import com.group7.dsl.thenFork
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
import kotlin.time.Duration.Companion.seconds

class ForkNodeTest :
    FunSpec({
        test("Vehicles in should equal vehicles out") {
            // Define channels
            val numForks = 4

            // Define nodes
            val sinks: List<SinkNode<TestVehicle>>

            runSimulation(
                buildScenario {
                    arrivals("Source", Generators.constant(TestVehicle, Delays.fixed(10.seconds)).take(NUM_VEHICLES))
                        .thenFork("Fork", numForks) { i, lane -> lane.thenSink("Sink $i") }
                        .let { sinks = it }
                }
            )

            sinks.sumOf { sink -> sink.reportMetrics().occupants ?: 0 } shouldBe NUM_VEHICLES
        }
    })
