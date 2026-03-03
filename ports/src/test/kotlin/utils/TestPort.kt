package com.group7.utils

import com.group7.Scenario
import com.group7.dsl.*
import com.group7.generators.Delays
import com.group7.generators.Generators
import com.group7.generators.take
import com.group7.policies.generic_fj.FirstAvailablePolicy
import com.group7.policies.generic_fj.forkPolicy
import com.group7.properties.Delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * A test port for golden data for metrics testing, is designed to have enough queues and buffers to draw data and
 * analyse data from.
 */
fun deterministicTestPort(): Scenario = buildScenario {
    arrivalsWithLoss(
            "Arriving into port",
            Generators.constant({ TestVehicleInstance() }, Delays.exponentialWithMean(40.seconds)).take(NUM_VEHICLES),
        )
        .thenBoundedQueue("Buffer 1", 15)
        .thenPump()
        .thenFork("Fork", 10, policy = forkPolicy(FirstAvailablePolicy())) { i, lane ->
            lane.thenService("ASC $i", Delays.exponentialWithMean(5.minutes))
        }
        .thenJoin("Join")
        .thenQueue("After service buffer")
        .thenService("Exit gate", Delays.exponentialWithMean(3.minutes), 6)
        .thenSink("Leaving port")
}

/** A test port with certain nodes converging to a desired value */
private class Truck

fun cyclicPort(delay: Duration, numVehicles: Int = 5000): Pair<Scenario, Delay<*>> {
    val road: Delay<*>
    return buildScenario {
        // Make a deferred push connection, equivalent concepts exist for pull channels
        val backEdge = newPushConnection<Truck>()

        val arrivals =
            arrivals("Truck Arrivals", Generators.constant(::Truck, Delays.fixed(10.minutes)).take(numVehicles))

        listOf(arrivals, backEdge)
            // Merge the new arrivals and the looping trucks
            .thenJoin("Trucks Merge")
            .thenDelay("Road", Delays.fixed(delay))
            .saveNode { road = it }
            // After the road, send trucks back round maybe
            .thenFork(
                "Truck Fork",
                listOf({ lane1 -> lane1.thenConnect(backEdge) }, { lane2 -> lane2.thenSink("Truck Departures") }),
            )
    } to road
}
