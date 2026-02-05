package com.group7

import com.group7.channels.ClosedChannelException
import com.group7.dsl.*
import com.group7.generators.Delays
import com.group7.nodes.QueueNode
import com.group7.nodes.SinkNode
import com.group7.utils.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

class MatchNodeTest :
    FunSpec({
        test("Should throw when no side input") {
            shouldThrow<ClosedChannelException> {
                val scenario = buildScenario {
                    arrivals("Source A", Presets.defaultFixedGenerator(2, obj = TestVehicle))
                        .thenMatch(
                            "Match",
                            arrivals("Source B", Presets.defaultFixedGenerator(0, obj = TestContainer))
                                .thenQueue("Queue"),
                        ) { _, _ ->
                            TestLoadedVehicle
                        }
                        .thenSink("Sink")
                }

                runSimulation(scenario)
            }
        }

        test("Should not pull when no main input") {
            val queue: QueueNode<TestContainer>
            val scenario = buildScenario {
                arrivals("Source A", Presets.defaultFixedGenerator(0, obj = TestVehicle))
                    .thenMatch(
                        "Match",
                        arrivals("Source B", Presets.defaultFixedGenerator(2, obj = TestContainer))
                            .thenQueue("Queue")
                            .saveNode { queue = it },
                    ) { _, _ ->
                        TestLoadedVehicle
                    }
                    .thenSink("Sink")
            }

            runSimulation(scenario)

            queue.occupants shouldBe 2
        }

        test("Processes inputs correctly") {
            val sink: SinkNode<TestLoadedVehicle>

            val scenario = buildScenario {
                arrivals("Source A", Presets.defaultFixedGenerator(1, obj = TestVehicle))
                    .thenDelay("Delay for input A", Delays.fixed(10.seconds))
                    .thenMatch(
                        "Match",
                        arrivals("Source B", Presets.defaultFixedGenerator(1, obj = TestContainer))
                            .thenQueue("Source B Queue"),
                    ) { _, _ ->
                        TestLoadedVehicle
                    }
                    .thenSink("Sink")
                    .let { sink = it }
            }

            runSimulation(scenario)

            sink.occupants shouldBe 1
        }
    })
