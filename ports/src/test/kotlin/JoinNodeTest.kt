package com.group7

import com.group7.channels.ClosedChannelException
import com.group7.channels.newPushChannel
import com.group7.dsl.buildScenario
import com.group7.dsl.thenJoin
import com.group7.dsl.thenSink
import com.group7.nodes.DeadEndNode
import com.group7.nodes.JoinNode
import com.group7.nodes.SinkNode
import com.group7.utils.DSLAddons.arrivalLanes
import com.group7.utils.Presets
import com.group7.utils.TestVehicle
import com.group7.utils.runSimulation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class JoinNodeTest :
    FunSpec({
        test("Vehicles going in should match vehicles going out") {
            val numVehiclesPerSource = 10
            val numSources = 10

            val sink: SinkNode<TestVehicle>

            runSimulation(
                buildScenario {
                    arrivalLanes(
                            generators =
                                List(numSources) {
                                    Presets.defaultFixedGenerator(numVehiclesPerSource, obj = TestVehicle)
                                }
                        ) { _, lane ->
                            lane
                        }
                        .thenJoin("Join")
                        .thenSink("Sink")
                        .let { sink = it }
                }
            )

            sink.occupants shouldBe numSources * numVehiclesPerSource
        }

        test("Channels should be blocked if the output is unavailable") {
            /**
             * If the output of the join node is blocked, then attempting to push into the join node should cause the
             * simulator to crash
             */
            shouldThrow<ClosedChannelException> {
                val numVehiclesPerSource = 10
                val numSources = 10
                val (scenario, inputChannels) =
                    Presets.generateSourcesWithGenerators(
                        List(numSources) { Presets.defaultFixedGenerator(numVehiclesPerSource, obj = TestVehicle) }
                    )
                val (joinOut, deadIn) = newPushChannel<TestVehicle>()

                // Define nodes
                JoinNode("Join", inputChannels, joinOut)
                DeadEndNode("DeadEnd", deadIn)

                runSimulation(scenario)
            }
        }
    })
