package com.group7

import com.group7.dsl.NodeBuilder
import com.group7.dsl.RegularNodeBuilder
import com.group7.dsl.arrivals
import com.group7.dsl.buildScenario
import com.group7.dsl.match
import com.group7.dsl.newConnection
import com.group7.dsl.thenConnect
import com.group7.dsl.thenDelay
import com.group7.dsl.thenFork
import com.group7.dsl.thenJoin
import com.group7.dsl.thenQueue
import com.group7.dsl.thenService
import com.group7.dsl.thenSink
import com.group7.dsl.thenSplit
import com.group7.generators.Delays
import com.group7.generators.Generators
import com.group7.generators.take
import com.group7.nodes.JoinNode
import com.group7.nodes.SinkNode
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
        val tokenBackEdge = newConnection<Token>()

        val truckQueue =
            arrivals(
                    "Truck Arrivals",
                    Generators.constant(Truck, Delays.exponential(truckArrivalsPerHour, DurationUnit.HOURS)).let {
                        if (numTrucks != null) it.take(numTrucks) else it
                    },
                )
                .thenQueue("Truck Arrival Queue")

        val tokenQueue = tokenBackEdge.thenQueue("Token Queue", List(numTokens) { Token })

        match("Token Match", truckQueue, tokenQueue) { truck, _ -> truck }
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
            .thenSplit("Token Split") { truck -> Pair(truck, Token) }
            .let { (trucks, tokens) ->
                sink = trucks.thenSink("Truck Departures")

                tokens.thenConnect(tokenBackEdge)
            }
    }

    return scenario to sink
}

private fun <T> NodeBuilder<T>.thenQueueAndGates(
    description: String,
    numLanes: Int,
    averageServiceTime: Duration,
): RegularNodeBuilder<JoinNode<T>, T> =
    this.thenQueue("$description Queue")
        .thenFork("$description Lane Split", numLanes) { i, lane ->
            lane.thenService("$description Gate $i", Delays.exponentialWithMean(averageServiceTime))
        }
        .thenJoin("$description Lane Join")
