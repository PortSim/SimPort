package com.group7

import LiveVisualisation
import MultiVisualisation
import StaticVisualisation
import components.MetricsPanelState
import java.util.stream.Collectors
import kotlin.time.Duration
import kotlinx.collections.immutable.toImmutableMap
import runVisualisation

fun runSimulation(scenario: Scenario, duration: Duration, logger: EventLog = EventLog.noop()) {
    val sampler = MetricsPanelState(scenario)
    val simulator = Simulator(logger, scenario, sampler)
    sampler.beginBatch()
    simulator.runFor(duration)
    sampler.endBatch()

    runVisualisation { StaticVisualisation(sampler) }
}

fun runSimulations(
    scenarios: Map<String, Scenario>,
    duration: Duration,
    logger: (String) -> EventLog = { EventLog.noop() },
) {
    val simulations =
        scenarios.entries
            .parallelStream()
            .map { (scenarioName, scenario) ->
                val sampler = MetricsPanelState(scenario)
                val simulator = Simulator(logger(scenarioName), scenario, sampler)
                sampler.beginBatch()
                simulator.runFor(duration)
                sampler.endBatch()
                scenarioName to sampler
            }
            .collect(
                Collectors.toMap(
                    { it.first },
                    { it.second },
                    { _, _ -> error("Duplicate simulation names!") },
                    ::mutableMapOf,
                )
            )
            .toImmutableMap()
    runVisualisation { MultiVisualisation(simulations) }
}

fun runLiveSimulation(scenario: Scenario, logger: EventLog = EventLog.noop()) {
    runVisualisation { LiveVisualisation(scenario, logger) }
}
