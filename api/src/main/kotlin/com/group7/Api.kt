package com.group7

import com.group7.state.SimulationState
import java.util.stream.Collectors
import kotlin.time.Duration
import kotlinx.collections.immutable.toImmutableMap

fun runSimulation(
    scenario: Scenario,
    duration: Duration,
    logger: EventLog = EventLog.noop(),
    iconProvider: IconProvider = IconProvider.defaultProvider(),
) {
    val sampler = SimulationState(scenario)
    val simulator = Simulator(logger, scenario, sampler)
    sampler.beginBatch()
    simulator.runFor(duration)
    sampler.endBatch()

    runVisualisation { StaticVisualisation(sampler, iconProvider = iconProvider) }
}

fun runSimulation(
    scenario: Scenario,
    events: Int,
    logger: EventLog = EventLog.noop(),
    iconProvider: IconProvider = IconProvider.defaultProvider(),
) {
    val sampler = SimulationState(scenario)
    val simulator = Simulator(logger, scenario, sampler)
    sampler.beginBatch()
    simulator.runFor(events)
    sampler.endBatch()

    runVisualisation { StaticVisualisation(sampler, iconProvider = iconProvider) }
}

fun runSimulations(
    scenarios: Map<String, Scenario>,
    duration: Duration,
    logger: (String) -> EventLog = { EventLog.noop() },
    iconProvider: IconProvider = IconProvider.defaultProvider(),
) {
    val simulations =
        scenarios.entries
            .parallelStream()
            .map { (scenarioName, scenario) ->
                val sampler = SimulationState(scenario)
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
    runVisualisation { MultiVisualisation(simulations, iconProvider) }
}

fun runLiveSimulation(
    scenario: Scenario,
    logger: EventLog = EventLog.noop(),
    iconProvider: IconProvider = IconProvider.defaultProvider(),
) {
    runVisualisation { LiveVisualisation(scenario, logger, iconProvider) }
}
