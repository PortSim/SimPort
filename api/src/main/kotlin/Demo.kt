package com.group7

import com.group7.dsl.*
import com.group7.generators.Delays
import com.group7.generators.Generators
import com.group7.metrics.Occupancy
import com.group7.metrics.ResponseTime
import com.group7.utils.thenSubnetwork
import kotlin.time.Duration.Companion.seconds

internal fun main() {
    val scenario =
        buildScenario {
                arrivalsWithLoss(
                        "Arrivals",
                        Generators.constant({ Vehicle(isLarge = true) }, Delays.exponentialWithMean(2.seconds)),
                    )
                    .thenBoundedQueue("Bounded buffer", 25)
                    .thenPushFork("Fork", 8) { i, lane ->
                        lane.thenSubnetwork("Subnetwork $i", 5) { lane ->
                            lane
                                .thenQueue("Internal buffer $i")
                                .thenService("Service $i", Delays.exponentialWithMean((16 * 2).seconds), numServers = 2)
                        }
                    }
                    .thenJoin("Join")
                    .thenSink("Sink")
            }
            .withMetrics {
                trackAll(Occupancy)
                trackAll(ResponseTime)
            }
    runLiveSimulation(scenario)
}
