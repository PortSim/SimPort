package com.group7

import com.group7.channels.ChannelType
import com.group7.dsl.*
import com.group7.generators.Delays
import com.group7.generators.Generators
import com.group7.generators.take
import com.group7.nodes.JoinNode
import com.group7.nodes.SinkNode
import com.group7.utils.thenSubnetwork
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

// Port layout based on figure 8 from the paper "Solving semi-open queuing networks with time-varying arrivals:
// An application in container terminal landside operations"

// Truck arrival (source node) ->
// Entrance gates (1 to m junction connected to 1 capacity roads (gates) connected to m to 1 junction) ->
// Travel to stacks (1 to N junction node) ->
// N crane platforms (each is a crane node) ->
// Travel to gates (road node) ->
// Exit gates (m lanes, modeled as an m capacity node) ->
// Truck leaves (sink node)

fun generatePort(
    entryGateLanes: Int = 6,
    exitGateLanes: Int = 6,
    numStackBlocks: Int = 29,
    truckArrivalsPerHour: Double = 50.0,
    averageGateServiceTime: Duration = 6.minutes,
    averageTravelTime: Duration = 5.6.minutes,
    averageHandlingTimeAtStack: Duration = 6.minutes,
    numTokens: Int = 30,
    numTrucks: Int? = null,
): Pair<Scenario, SinkNode<Truck>> {
    val sink: SinkNode<Truck>

    val scenario = buildScenario {
        arrivals(
                "Truck Arrivals",
                Generators.constant(Truck, Delays.exponential(truckArrivalsPerHour, DurationUnit.HOURS)).let {
                    if (numTrucks != null) it.take(numTrucks) else it
                },
            )
            .thenQueue("Truck Arrival Queue")
            .thenSubnetwork(capacity = numTokens) { entrance ->
                entrance
                    .thenQueueAndGates("Entrance", entryGateLanes, averageGateServiceTime)
                    .thenDelay("Travel to stacks", Delays.exponentialWithMean(averageTravelTime))
                    .thenFork("ASC Split", numStackBlocks) { i, lane ->
                        lane
                            .thenQueue("ASC Queue $i")
                            .thenService("ASC $i", Delays.exponentialWithMean(averageHandlingTimeAtStack))
                    }
                    .thenJoin("ASC Join")
                    .thenDelay("Travel to gates", Delays.exponentialWithMean(averageTravelTime))
                    .thenQueueAndGates("Exit", exitGateLanes, averageGateServiceTime)
            }
            .thenSink("Truck Departures")
            .let { sink = it }
    }

    return scenario to sink
}

context(_: GroupScope)
private fun <T> NodeBuilder<T, *>.thenQueueAndGates(
    description: String,
    numLanes: Int,
    averageServiceTime: Duration,
): RegularNodeBuilder<JoinNode<T>, T, ChannelType.Push> =
    this.thenQueue("$description Queue")
        .thenPump()
        .thenFork("$description Lane Split", numLanes) { i, lane ->
            lane.thenService("$description Gate $i", Delays.exponentialWithMean(averageServiceTime))
        }
        .thenJoin("$description Lane Join")
