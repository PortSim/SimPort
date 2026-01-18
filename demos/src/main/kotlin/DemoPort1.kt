package com.group7

import kotlin.time.Duration.Companion.seconds

// Port layout based on figure the paper "Solving semi-open queuing networks with time-varying arrivals:
// An application in container terminal landside operations"

// Truck arrival (source node) ->
// Entrance gates (1 to m junction connected to 1 capacity roads (gates) connected to m to 1 junction) ->
// Travel to stacks (1 to N junction node) ->
// N crane platforms (each is a crane node) ->
// Travel to gates (road node) ->
// Exit gates (m lanes, modeled as an m capacity node) ->
// Truck leaves (sink node)

fun generatePort(gateSize: Int, craneNum: Int, numTrucks: Int): Pair<Simulator, Sink<RoadObject>> {
    val (sourceOutput, roadToGatesInput) = newChannel<RoadObject>()
    val source = GeneratorSource("Source", sourceOutput, Generators.exponentialDelay(Truck, 1.0).take(numTrucks))

    val (roadToGatesOutput, junctionToGatesInput) = newChannel<RoadObject>()
    val roadToGates = RoadNode("Road To Gates", roadToGatesInput, roadToGatesOutput, 100, 5.seconds)

    val (junctionToGatesOutputs, entranceGatesInputs) = newChannels<RoadObject>(gateSize)
    val junctionToGates =
        JunctionNode("Junction To Gates", listOf(junctionToGatesInput), junctionToGatesOutputs, 10, 5.seconds)

    val (entranceGatesOutputs, junctionFromGatesInputs) = newChannels<RoadObject>(gateSize)
    val entranceGates =
        List(gateSize) { i ->
            RoadNode("Entrance Gate $i", entranceGatesInputs[i], entranceGatesOutputs[i], 1, 60.seconds)
        }

    val (junctionFromGatesOutput, roadToCranesInput) = newChannel<RoadObject>()
    val junctionFromGates =
        JunctionNode("Junction From Gates", junctionFromGatesInputs, listOf(junctionFromGatesOutput), 10, 5.seconds)

    val (roadToCranesOutput, junctionToCranesInput) = newChannel<RoadObject>()
    val roadToCranes = RoadNode("Road To Cranes", roadToCranesInput, roadToCranesOutput, 10, 5.seconds)

    val (junctionToCranesOutputs, craneInputs) = newChannels<RoadObject>(craneNum)
    val junctionToCranes =
        JunctionNode(
            "Junction To Crane Platforms",
            listOf(junctionToCranesInput),
            junctionToCranesOutputs,
            10,
            5.seconds,
        )

    // TODO() cranes currently modelled as roads, need a separate node later
    val (craneOutputs, junctionFromCranesInputs) = newChannels<RoadObject>(craneNum)
    val cranePlatforms = List(craneNum) { i -> RoadNode("Crane $i", craneInputs[i], craneOutputs[i], 1, 60.seconds) }

    val (junctionFromCranesOutput, roadToExitGatesInput) = newChannel<RoadObject>()
    val junctionFromCranes =
        JunctionNode(
            "Junction From Crane Platforms",
            junctionFromCranesInputs,
            listOf(junctionFromCranesOutput),
            10,
            5.seconds,
        )

    val (roadToExitGatesOutput, junctionToExitGatesInput) = newChannel<RoadObject>()
    val roadToExitGates = RoadNode("Road To Exit Gates", roadToExitGatesInput, roadToExitGatesOutput, 10, 5.seconds)

    val (junctionToExitGatesOutputs, exitGatesInputs) = newChannels<RoadObject>(gateSize)
    val junctionToExitGates =
        JunctionNode(
            "Junction To Exit Gates",
            listOf(junctionToExitGatesInput),
            junctionToExitGatesOutputs,
            10,
            5.seconds,
        )

    val (exitGatesOutputs, junctionFromExitGatesInputs) = newChannels<RoadObject>(gateSize)
    val exitGates =
        List(gateSize) { i -> RoadNode("Exit Gate $i", exitGatesInputs[i], exitGatesOutputs[i], 1, 60.seconds) }

    val (junctionFromExitGatesOutput, roadToSinkInput) = newChannel<RoadObject>()
    val junctionFromExitGates =
        JunctionNode(
            "Junction From Exit Gates",
            junctionFromExitGatesInputs,
            listOf(junctionFromExitGatesOutput),
            10,
            5.seconds,
        )

    val (roadToSinkOutput, sinkInput) = newChannel<RoadObject>()
    val roadToSink = RoadNode("Road Out Of Port", roadToSinkInput, roadToSinkOutput, 10, 5.seconds)

    val sink = Sink("Sink", listOf(sinkInput))

    val scenario = Scenario(source)

    val simulator = Simulator(EventLog.noop(), scenario)

    return Pair(simulator, sink)
}
