package com.group7.joins

import com.group7.channels.ClosedChannelException
import com.group7.dsl.*
import com.group7.generators.Delays
import com.group7.generators.Generators
import com.group7.generators.take
import com.group7.nodes.SinkNode
import com.group7.utils.*
import com.group7.utils.DSLAddons.arrivalLanes
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds
import kotlin.time.times

class PullJoinNodeTest :
    FunSpec({
        test("Vehicles in should be vehicles out") {
            val numLanes = 10
            val delay = 10.seconds
            val sink: SinkNode<TestVehicle>

            val (qlog, startTime) =
                runSimulation(
                    buildScenario {
                        arrivalLanes(
                                List(numLanes) {
                                    Generators.constant(TestVehicle, Delays.fixed(delay)).take(NUM_VEHICLES / numLanes)
                                }
                            ) { i, lane ->
                                lane.thenQueue("Queue $i")
                            }
                            .thenJoin("Pull Join")
                            .thenService("Service", Delays.fixed(delay))
                            .thenSink("Sink")
                            .let { sink = it }
                    },
                    log = QueryLog(),
                )

            sink.occupants shouldBe NUM_VEHICLES
            qlog.query("Sink", VehicleTravelDirection.INBOUND).last() shouldBe startTime + ((NUM_VEHICLES + 1) * delay)

            shouldThrow<ClosedChannelException> {
                runSimulation(
                    buildScenario {
                        arrivalLanes(
                                List(numLanes) {
                                    Generators.constant(TestVehicle, Delays.fixed(delay)).take(NUM_VEHICLES / numLanes)
                                }
                            ) { _, lane ->
                                lane
                            }
                            .thenJoin("Pull Join")
                            .thenService("Service", Delays.fixed(delay))
                            .thenSink("Sink")
                    }
                )
            }
        }
    })
