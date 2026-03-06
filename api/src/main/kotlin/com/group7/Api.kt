package com.group7

import com.group7.state.SimulationState
import java.util.stream.Collectors
import kotlin.time.Duration
import kotlinx.collections.immutable.toImmutableMap

/**
 * Runs a single simulation for a specified duration and displays the results.
 *
 * The simulation is executed until the specified duration of simulation time has elapsed, then the final state is
 * displayed with a static visualization.
 *
 * @param scenario the [Scenario] to simulate
 * @param duration the simulation time to run for
 * @param logger the [EventLog] for recording simulation events (default: no-op)
 * @param iconProvider the [IconProvider] for custom node icons (default: default provider)
 */
fun runSimulation(
    scenario: Scenario,
    duration: Duration,
    logger: EventLog = EventLog.noop(),
    iconProvider: IconProvider = IconProvider.defaultProvider(),
) {
    val sampler = SimulationState(scenario, iconProvider)
    val simulator = Simulator(logger, scenario, sampler)
    sampler.beginBatch()
    simulator.runFor(duration)
    sampler.endBatch()

    runVisualisation { StaticVisualisation(sampler) }
}

/**
 * Runs a single simulation for a specified number of events and displays the results.
 *
 * The simulation is executed until the specified number of events have been processed, then the final state is
 * displayed with a static visualization.
 *
 * @param scenario the [Scenario] to simulate
 * @param events the number of events to process
 * @param logger the [EventLog] for recording simulation events (default: no-op)
 * @param iconProvider the [IconProvider] for custom node icons (default: default provider)
 */
fun runSimulation(
    scenario: Scenario,
    events: Int,
    logger: EventLog = EventLog.noop(),
    iconProvider: IconProvider = IconProvider.defaultProvider(),
) {
    val sampler = SimulationState(scenario, iconProvider)
    val simulator = Simulator(logger, scenario, sampler)
    sampler.beginBatch()
    simulator.runFor(events)
    sampler.endBatch()

    runVisualisation { StaticVisualisation(sampler) }
}

/**
 * Runs multiple simulations in parallel and displays comparative results.
 *
 * Each [Scenario] is simulated for the specified duration in parallel, then all results are displayed together in a
 * multi-simulation visualization for comparison.
 *
 * @param scenarios a map of scenario names to [Scenario] objects to simulate
 * @param duration the simulation time to run each scenario for
 * @param logger a factory function to create an [EventLog] for each scenario (default: no-op)
 * @param iconProvider the [IconProvider] for custom node icons (default: default provider)
 */
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
                val sampler = SimulationState(scenario, iconProvider)
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

/**
 * Runs a simulation in real-time and displays it with a live visualization.
 *
 * Unlike the static simulation runners, this allows the simulation to be stepped and visualized interactively as it
 * progresses.
 *
 * @param scenario the [Scenario] to simulate
 * @param logger the [EventLog] for recording simulation events (default: no-op)
 * @param iconProvider the [IconProvider] for custom node icons (default: default provider)
 */
fun runLiveSimulation(
    scenario: Scenario,
    logger: EventLog = EventLog.noop(),
    iconProvider: IconProvider = IconProvider.defaultProvider(),
) {
    runVisualisation { LiveVisualisation(scenario, logger, iconProvider) }
}
