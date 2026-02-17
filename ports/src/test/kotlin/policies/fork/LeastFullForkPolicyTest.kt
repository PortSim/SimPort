package com.group7.policies.fork

import com.group7.dsl.*
import com.group7.generators.Delays
import com.group7.utils.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ranges.shouldBeIn
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LeastFullForkPolicyTest :
    FunSpec({
        test("Should forward all vehicles to least full tagged container") {
            val (qlog, startTime) =
                runSimulation(
                    buildScenario {
                        Presets.defaultArrivals({ TestVehicle })
                            .thenQueue("Queue")
                            .thenPump()
                            .thenFork("Fork", 3, policy = LeastFullForkPolicy<TestVehicle>()) { i, lane ->
                                if (i <= 1) {
                                    lane.thenSubnetwork("Congested subnetwork $i", 5) {
                                        it.thenQueue("Queue $i")
                                            .thenService("Congested service $i", Delays.fixed(5.hours))
                                    }
                                } else {
                                    lane.thenSubnetwork("Fast service subnetwork", 5) {
                                        it.thenQueue("Queue $i").thenService("Fast service", Delays.fixed(5.seconds))
                                    }
                                }
                            }
                            .thenJoin("Join")
                            .thenSink("Sink")
                    },
                    log = QueryLog(),
                )

            // Some grace is allowed, as the other two slow subnetworks may have one vehicle dispatched. But once that
            // one vehicle is in the slow subnetwork, then the fast subnetwork will always be the less full subnetwork
            qlog.query("Queue 2", VehicleTravelDirection.INBOUND).size shouldBeIn (NUM_VEHICLES - 2)..NUM_VEHICLES
            qlog.query("Sink", VehicleTravelDirection.INBOUND).size shouldBe NUM_VEHICLES

            //  All vehicles should pass thorugh the network in no more than 5 hours and 10 seconds (for vehicles
            // dispatched into the slow lanes)
            (qlog.query("Sink", VehicleTravelDirection.INBOUND).last() - startTime) shouldBeIn
                (16.minutes + 45.seconds)..(5.hours + 20.seconds)
        }

        test("Complex subnetworking pattern") {
            runSimulation(
                buildScenario {
                    Presets.defaultArrivals({ TestVehicle })
                        .thenQueue("Queue")
                        .thenPump()
                        .thenFork("Fork", 3, policy = LeastFullForkPolicy<TestVehicle>()) { i, lane ->
                            if (i <= 1) {
                                lane.thenSubnetwork("Congested subnetwork $i", 5) {
                                    it.thenQueue("Queue $i")
                                        .thenService("Congested service $i", Delays.fixed(35.seconds))
                                }
                            } else {
                                lane.thenSubnetwork("Fast service subnetwork", 5) {
                                    it.thenQueue("Queue $i").thenService("Fast service", Delays.fixed(20.seconds))
                                }
                            }
                        }
                        .thenJoin("Join")
                        .thenSink("Sink")
                },
                log = QueryLog(),
            )
        }
    }) {}
