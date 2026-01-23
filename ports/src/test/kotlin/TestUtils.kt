package com.group7

import com.group7.dsl.RegularNodeBuilder
import com.group7.dsl.ScenarioBuilderScope
import com.group7.dsl.arrivals
import com.group7.generators.*
import com.group7.nodes.ArrivalNode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** A collection of useful functions, objects, and classes to be used within Node Tests */
internal data object TestVehicle

internal data object TestContainer

internal data object TestLoadedVehicle

internal const val NUM_VEHICLES = 100

internal object TestDelays {
    fun customDelays(delays: List<Duration>): Generator<TestVehicle> =
        delays.asSequence().map { TestVehicle to it }.asGenerator()
}

/** A log that keeps track of time of the events, without keeping the message associated with each event */
internal class TimeLog : EventLog {
    private val _timeLog = mutableListOf<Instant>()

    val timeLog: List<Instant>
        get() = _timeLog

    override fun log(time: Instant, message: () -> String) {
        _timeLog += time
    }
}

/** A log that can be queried later on list of timestamps for a specific event occuring on a specific node */
internal class QueryLog : EventLog {
    private val log = mutableListOf<Pair<Instant, String>>()

    override fun log(time: Instant, message: () -> String) {
        log.add(Pair(time, message()))
    }

    fun query(nodeLabel: String, directionality: VehicleTravelDirection): List<Instant> {
        val matchText =
            when (directionality) {
                VehicleTravelDirection.OUTBOUND -> "from "
                VehicleTravelDirection.INBOUND -> "to "
            } + nodeLabel

        return log.asSequence().filter { (_, loggedEvent) -> matchText in loggedEvent }.map { it.first }.toList()
    }
}

internal enum class VehicleTravelDirection {
    OUTBOUND,
    INBOUND,
}

internal object Presets {
    /**
     * Generates n number of sources, each assigned with a default generator generating 10 vehicles if no generator
     * provided
     */
    fun <T> generateSourcesWithGenerators(generators: List<Generator<T>>): Pair<Scenario, List<InputChannel<T>>> {
        val (sourceOuts, inputChannels) = newChannels<T>(generators.size)
        val sources = sourceOuts.zip(generators) { sourceOut, generator -> ArrivalNode("Source", sourceOut, generator) }
        return Scenario(sources) to inputChannels
    }

    /**
     * Provided with only one generator, returns scenario and input channel to connect with the next node after the
     * source
     */
    fun <T> generateSourcesWithGenerator(generator: Generator<T>): Pair<Scenario, InputChannel<T>> {
        val (scenario, singleton) = generateSourcesWithGenerators(listOf(generator))
        return scenario to singleton[0]
    }

    /**
     * Generates a fixed generator of vehicles. The type of vehicle to emit by the generator has to be passed in
     * manually, but separation time between vehicle dispatches are by default 10 seconds
     */
    fun <T> defaultFixedGenerator(numCars: Int, obj: T, separationTime: Duration = 10.seconds): Generator<T> =
        Generators.constant(obj, Delays.fixed(separationTime)).take(numCars)
}

/**
 * Runs a simulation to completion or until a specific time duration has passed, based on the parameters passed in
 *
 * Returns the completed log (by default generates a TimeLog), along with simulation start time for direct log message
 * comparing
 */
internal fun <LoggerType : EventLog> runSimulation(
    scenario: Scenario,
    log: LoggerType,
    timeConstraint: Duration = Duration.INFINITE,
): Pair<LoggerType, Instant> {
    val simulator = Simulator(log, scenario)
    val startTime = simulator.currentTime
    val endTime = startTime + timeConstraint
    while (!simulator.isFinished && (simulator.nextEventTime ?: Instant.DISTANT_FUTURE) < endTime) {
        simulator.nextStep()
    }
    return log to startTime
}

internal fun runSimulation(scenario: Scenario, timeConstraint: Duration = Duration.INFINITE) =
    runSimulation(scenario, TimeLog(), timeConstraint)

/**
 * Generates parallel arrival lanes which can then be processed independently, and joined after each lane passes through
 * some stages
 */
context(_: ScenarioBuilderScope)
fun <T, R> arrivalLanes(
    generators: List<Generator<T>>,
    laneAction: (Int, RegularNodeBuilder<ArrivalNode<T>, T>) -> R,
): List<R> {
    return generators.mapIndexed { index, generator -> laneAction(index, arrivals("Arrival $index", generator)) }
}
