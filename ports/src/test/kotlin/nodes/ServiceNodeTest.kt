package com.group7.nodes

import com.group7.dsl.*
import com.group7.generators.Delays
import com.group7.utils.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds
import kotlin.time.times

private val SERVICE_TIME = 10.seconds
private val SEPARATION_TIME = 1.seconds

class ServiceNodeTest :
    FunSpec({
        test("All vehicles entering should come out according to the service time") {
            val sink: SinkNode<TestVehicle>
            val (qlog, startTime) =
                runSimulation(
                    buildScenario {
                        arrivals(
                                "Arrival",
                                generator =
                                    Presets.defaultFixedGenerator(
                                        NUM_VEHICLES,
                                        { TestVehicle },
                                        separationTime = SEPARATION_TIME,
                                    ),
                            )
                            .thenQueue("Queue")
                            .thenService("Service", Delays.fixed(SERVICE_TIME))
                            .thenSink("Sink")
                            .let { sink = it }
                    },
                    QueryLog(),
                )

            sink.occupants shouldBe NUM_VEHICLES
            qlog.query("Sink", VehicleTravelDirection.INBOUND) shouldBe
                List(NUM_VEHICLES) { startTime + ((it + 1) * SERVICE_TIME) + SEPARATION_TIME }
        }

        test("Parallel service node should increase throughput") {
            val numParallelServices = (SERVICE_TIME / SEPARATION_TIME).toInt()
            val timeConstraint = 30.seconds
            val (qlog, _) =
                runSimulation(
                    buildScenario {
                        // Generate a lot of vehicles, as we are concerned with throughput not whether the vehicles make
                        // it all through
                        arrivals(
                                "Arrival",
                                Presets.defaultFixedGenerator(
                                    NUM_VEHICLES * NUM_VEHICLES,
                                    { TestVehicle },
                                    separationTime = SEPARATION_TIME / 2,
                                ),
                            )
                            .thenQueue("Queue")
                            .thenPump()
                            .thenFork("Fork", numParallelServices) { i, lane ->
                                lane.thenService("Service $i", Delays.fixed(SERVICE_TIME))
                            }
                            .thenJoin("Join")
                            .thenSink("Sink")
                    },
                    QueryLog(),
                    timeConstraint,
                )

            // Because vehicles start dispatching at t = 1 instead of t = 0, when time reaches timeConstraint we are
            // missing one round
            qlog.query("Sink", VehicleTravelDirection.INBOUND).size shouldBe
                (numParallelServices * ((timeConstraint / SERVICE_TIME) - 1)).toInt()
        }
    })
