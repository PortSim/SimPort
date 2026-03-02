package com.group7.nodes.forks

import com.group7.channels.ClosedChannelException
import com.group7.dsl.*
import com.group7.generators.Delays
import com.group7.generators.Generators
import com.group7.generators.take
import com.group7.nodes.SinkNode
import com.group7.utils.NUM_VEHICLES
import com.group7.utils.TestVehicle
import com.group7.utils.runSimulation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

// Define channels
private const val numForks = 4

class PushForkNodeTest :
    FunSpec({
        test("Vehicles in should equal vehicles out") {
            // Define nodes
            val sinks: List<SinkNode<TestVehicle>>

            runSimulation(
                buildScenario {
                    arrivals(
                            "Source",
                            Generators.constant({ TestVehicle }, Delays.fixed(10.seconds)).take(NUM_VEHICLES),
                        )
                        .thenFork("Fork", numForks) { i, lane -> lane.thenSink("Sink $i") }
                        .let { sinks = it }
                }
            )

            sinks.sumOf { sink -> sink.occupants } shouldBe NUM_VEHICLES
        }

        test("Fork should fail if all output channels are closed") {
            shouldThrow<ClosedChannelException> {
                runSimulation(
                    buildScenario {
                        arrivals(
                                "Source",
                                Generators.constant({ TestVehicle }, Delays.fixed(10.seconds)).take(NUM_VEHICLES),
                            )
                            .thenDelay("Some upstream activity", Delays.fixed(5.seconds))
                            .thenFork("Fork node", numForks) { _, lane ->
                                lane.thenService(
                                    "Service nodes too slow to handle traffic",
                                    Delays.fixed(15.seconds * numForks),
                                )
                            }
                            .thenJoin("Join")
                            .thenSink("Sink")
                    }
                )
            }
        }
    })
