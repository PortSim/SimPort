package com.group7

import com.group7.generators.Delays
import com.group7.nodes.ArrivalNode
import com.group7.nodes.DelayNode
import com.group7.nodes.MatchNode
import com.group7.nodes.SinkNode
import com.group7.utils.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

class MatchNodeTest :
    FunSpec({
        test("Only take one of input A") {
            shouldThrow<ClosedChannelException> {
                val (source1Out, matchIn1) = newChannel<TestVehicle>()
                val (source2Out, matchIn2) = newChannel<TestContainer>()
                val (matchOut, sinkIn) = newChannel<TestLoadedVehicle>()

                val sources =
                    listOf(
                        ArrivalNode("Source A", source1Out, Presets.defaultFixedGenerator(2, obj = TestVehicle)),
                        ArrivalNode("Source B", source2Out, Presets.defaultFixedGenerator(0, obj = TestContainer)),
                    )

                SinkNode("Sink", sinkIn)

                MatchNode<TestVehicle, TestContainer, TestLoadedVehicle>(
                    "Match",
                    sourceA = matchIn1,
                    sourceB = matchIn2,
                    destination = matchOut,
                    combiner = { _, _ -> TestLoadedVehicle },
                )

                runSimulation(Scenario(sources))
            }
        }

        test("Only take one of input B") {
            shouldThrow<ClosedChannelException> {
                val (source1Out, matchIn1) = newChannel<TestVehicle>()
                val (source2Out, matchIn2) = newChannel<TestContainer>()
                val (matchOut, sinkIn) = newChannel<TestLoadedVehicle>()

                val sources =
                    listOf(
                        ArrivalNode("Source A", source1Out, Presets.defaultFixedGenerator(0, obj = TestVehicle)),
                        ArrivalNode("Source B", source2Out, Presets.defaultFixedGenerator(2, obj = TestContainer)),
                    )

                SinkNode("Sink", sinkIn)

                MatchNode<TestVehicle, TestContainer, TestLoadedVehicle>(
                    "Match",
                    sourceA = matchIn1,
                    sourceB = matchIn2,
                    destination = matchOut,
                    combiner = { _, _ -> TestLoadedVehicle },
                )

                runSimulation(Scenario(sources))
            }
        }

        test("Stores one of input A") {
            val (source1Out, matchIn1) = newChannel<TestVehicle>()
            val (source2Out, matchIn2) = newChannel<TestContainer>()
            val (matchOut, sinkIn) = newChannel<TestLoadedVehicle>()

            val sources =
                listOf(
                    ArrivalNode("Source A", source1Out, Presets.defaultFixedGenerator(1, obj = TestVehicle)),
                    ArrivalNode("Source B", source2Out, Presets.defaultFixedGenerator(0, obj = TestContainer)),
                )

            val sink = SinkNode("Sink", sinkIn)

            MatchNode<TestVehicle, TestContainer, TestLoadedVehicle>(
                "Match",
                sourceA = matchIn1,
                sourceB = matchIn2,
                destination = matchOut,
                combiner = { _, _ -> TestLoadedVehicle },
            )

            runSimulation(Scenario(sources))

            sink.occupants shouldBe 0
        }

        test("Stores one of input B") {
            val (source1Out, matchIn1) = newChannel<TestVehicle>()
            val (source2Out, matchIn2) = newChannel<TestContainer>()
            val (matchOut, sinkIn) = newChannel<TestLoadedVehicle>()

            val sources =
                listOf(
                    ArrivalNode("Source A", source1Out, Presets.defaultFixedGenerator(0, obj = TestVehicle)),
                    ArrivalNode("Source B", source2Out, Presets.defaultFixedGenerator(1, obj = TestContainer)),
                )

            val sink = SinkNode("Sink", sinkIn)

            MatchNode<TestVehicle, TestContainer, TestLoadedVehicle>(
                "Match",
                sourceA = matchIn1,
                sourceB = matchIn2,
                destination = matchOut,
                combiner = { _, _ -> TestLoadedVehicle },
            )

            runSimulation(Scenario(sources))

            sink.occupants shouldBe 0
        }

        test("Processes inputs correctly") {
            val (source1Out, delayIn) = newChannel<TestVehicle>()
            val (delayOut, matchIn1) = newChannel<TestVehicle>()
            val (source2Out, matchIn2) = newChannel<TestContainer>()
            val (matchOut, sinkIn) = newChannel<TestLoadedVehicle>()

            val sources =
                listOf(
                    ArrivalNode("Source A", source1Out, Presets.defaultFixedGenerator(1, obj = TestVehicle)),
                    ArrivalNode("Source B", source2Out, Presets.defaultFixedGenerator(1, obj = TestContainer)),
                )

            val sink = SinkNode("Sink", sinkIn)

            // Delay input A a little bit
            DelayNode("Delay for input A", delayIn, delayOut, Delays.fixed(10.seconds))

            MatchNode(
                "Match",
                sourceA = matchIn1,
                sourceB = matchIn2,
                destination = matchOut,
                combiner = { _, _ -> TestLoadedVehicle },
            )

            runSimulation(Scenario(sources))

            sink.occupants shouldBe 1
        }
    })
