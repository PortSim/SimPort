package com.group7.utils

import com.group7.EventLog
import com.group7.Scenario
import com.group7.Simulator
import kotlin.time.Duration
import kotlin.time.Instant

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
