package com.group7.nodes.forks

import com.group7.dsl.*
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

class PullForkNodeTest :
    FunSpec({
        test("Vehicles in should equal vehicles out") {
            // Define channels
            val numForks = 4

            // Define nodes
            val sinks: List<SinkNode<TestVehicle>>

            runSimulation(
                buildScenario {
                    arrivals(
                            "Source",
                            Generators.constant({ TestVehicle }, Delays.fixed(10.seconds)).take(NUM_VEHICLES),
                        )
                        .thenQueue("Queue")
                        .thenPushFork("Fork", numForks) { i, lane -> lane.thenSink("Sink") }
                        .let { sinks = it }
                }
            )

            sinks.sumOf { it.occupants } shouldBe NUM_VEHICLES
        }
    })
