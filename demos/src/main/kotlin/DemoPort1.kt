package com.group7

import com.group7.generators.Delays
import com.group7.generators.Generators
import com.group7.generators.take
import com.group7.nodes.ArrivalNode
import com.group7.nodes.DelayNode
import com.group7.nodes.ForkNode
import com.group7.nodes.JoinNode
import com.group7.nodes.MatchNode
import com.group7.nodes.QueueNode
import com.group7.nodes.ServiceNode
import com.group7.nodes.SinkNode
import com.group7.nodes.SplitNode
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
    truckArrivalsPerHour: Double = 60.0,
    averageGateServiceTime: Duration = 6.minutes,
    averageTravelTime: Duration = 5.6.minutes,
    averageHandlingTimeAtStack: Duration = 6.minutes,
    numTokens: Int = 30,
    numTrucks: Int? = null,
): Pair<Scenario, SinkNode<Truck>> {
    val (arrivalOutput, arrivalQueueInput) = newChannel<Truck>()
    val arrivals =
        ArrivalNode(
            "Truck Arrivals",
            arrivalOutput,
            Generators.constant(Truck, Delays.exponential(truckArrivalsPerHour, DurationUnit.HOURS)).let {
                if (numTrucks != null) it.take(numTrucks) else it
            },
        )

    val (arrivalQueueOutput, tokenMatchTruckInput) = newChannel<Truck>()
    QueueNode("Truck Arrival Queue", arrivalQueueInput, arrivalQueueOutput)

    val (tokenSplitTokenOutput, tokenQueueInput) = newChannel<Token>()
    val (tokenQueueOutput, tokenMatchTokenInput) = newChannel<Token>()
    QueueNode("Token Queue", tokenQueueInput, tokenQueueOutput, List(numTokens) { Token })

    val (tokenMatchOutput, entranceQueueInput) = newChannel<Truck>()
    MatchNode("Token Match", tokenMatchTruckInput, tokenMatchTokenInput, tokenMatchOutput) { truck, _ -> truck }

    val travelToStacksInput = makeGatesWithQueue("Entrance", entranceQueueInput, entryGateLanes, averageGateServiceTime)

    val (travelToStacksOutput, stacksInput) = newChannel<Truck>()
    DelayNode(
        "Travel to stacks",
        travelToStacksInput,
        travelToStacksOutput,
        Delays.exponentialWithMean(averageTravelTime),
    )

    val travelToGatesInput =
        makeLanes("ASC", stacksInput, numStackBlocks) { i, input, output ->
            val (queueOutput, craneInput) = newChannel<Truck>()
            QueueNode("ASC Queue $i", input, queueOutput)

            ServiceNode("ASC $i", craneInput, output, Delays.exponentialWithMean(averageHandlingTimeAtStack))
        }

    val (travelToGatesOutput, exitQueueInput) = newChannel<Truck>()
    DelayNode("Travel to gates", travelToGatesInput, travelToGatesOutput, Delays.exponentialWithMean(averageTravelTime))

    val tokenSplitInput = makeGatesWithQueue("Exit", exitQueueInput, exitGateLanes, averageGateServiceTime)

    val (tokenSplitTruckOutput, sinkInput) = newChannel<Truck>()
    SplitNode("Token Split", tokenSplitInput, tokenSplitTruckOutput, tokenSplitTokenOutput) { truck ->
        Pair(truck, Token)
    }

    return Scenario(arrivals) to SinkNode("Truck Departures", listOf(sinkInput))
}

private fun <T> makeGatesWithQueue(
    description: String,
    source: InputChannel<T>,
    numLanes: Int,
    averageServiceTime: Duration,
): InputChannel<T> {
    val (queueOutput, forkInput) = newChannel<T>()
    QueueNode("$description Queue", source, queueOutput)
    return makeLanes(description, forkInput, numLanes) { i, input, output ->
        ServiceNode("$description Gate $i", input, output, Delays.exponentialWithMean(averageServiceTime))
    }
}

private fun <T> makeLanes(
    description: String,
    source: InputChannel<T>,
    numLanes: Int,
    makeLane: (Int, InputChannel<T>, OutputChannel<T>) -> Unit,
): InputChannel<T> {
    val (queueOutputs, laneInputs) = newChannels<T>(numLanes)
    ForkNode("$description Lane Split", source, queueOutputs)

    val (laneOutputs, joinInputs) = newChannels<T>(numLanes)
    for ((i, channels) in laneInputs.asSequence().zip(laneOutputs.asSequence()).withIndex()) {
        val (input, output) = channels
        makeLane(i, input, output)
    }

    val (joinOutput, resultInput) = newChannel<T>()
    JoinNode("$description Lane Join", joinInputs, joinOutput)

    return resultInput
}
