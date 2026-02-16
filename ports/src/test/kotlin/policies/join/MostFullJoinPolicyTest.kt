package com.group7.policies.join

import com.group7.dsl.*
import com.group7.generators.Delays
import com.group7.policies.fork.MostFullJoinPolicy
import com.group7.utils.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ranges.shouldBeIn
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class MostFullJoinPolicyTest :
    FunSpec({
        test("Should pull vehicles from the most filled container") {
            val (qlog, _) =
                runSimulation(
                    buildScenario {
                        Presets.defaultArrivals(TestVehicle)
                            .thenQueue("Queue")
                            .thenFork("Fork", 3) { i, lane ->
                                // One lane that has greater capacity with a bigger buffer, and two lanes that services
                                // items very slowly and smaller capacity.
                                if (i <= 1) {
                                    lane.thenSubnetwork("Slow subnetwork $i", capacity = 3) {
                                        it.thenService("Slow service $i", Delays.fixed(5.hours))
                                            .thenQueue("Buffer for slow subnetwork $i")
                                    }
                                } else {
                                    lane.thenSubnetwork("Fast high capacity subnetwork", capacity = 100) {
                                        it.thenService("Fast service $i", Delays.fixed(5.seconds))
                                            .thenQueue("Buffer for fast subnetwork")
                                    }
                                }
                            }
                            .thenJoin("Pull join node", policy = MostFullJoinPolicy())
                            .thenSink("Sink")
                    },
                    log = QueryLog(),
                )

            qlog.query("Buffer for fast subnetwork", VehicleTravelDirection.INBOUND).size shouldBeIn
                (NUM_VEHICLES - 2)..NUM_VEHICLES
            qlog.query("Sink", VehicleTravelDirection.INBOUND).size shouldBe NUM_VEHICLES
        }
    }) {}
