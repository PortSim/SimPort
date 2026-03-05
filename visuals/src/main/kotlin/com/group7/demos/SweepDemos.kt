package com.group7.demos

import com.group7.*
import com.group7.state.SimulationState
import kotlin.time.Duration.Companion.days
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap

fun demoCraneNumberSweep() = sweep((1..20).map { "$it Cranes" to generatePort(6, 6, it).first })

fun demoArrivalRateSweep() =
    sweep((10..60 step 5).map { "$it/h" to generatePort(truckArrivalsPerHour = it.toDouble()).first })

fun demoPolicySweep() =
    sweep(
        DemoQueuePolicy.entries.flatMap { queuePolicy ->
            DemoForkPolicy.entries.map { forkPolicy ->
                "$queuePolicy, $forkPolicy" to policyDemoPort(queuePolicy, forkPolicy)
            }
        }
    )

private fun sweep(scenarios: List<Pair<String, Scenario>>): ImmutableMap<String, SimulationState> {
    // Run simulations
    val runFor = 5.days
    return scenarios
        .associate { (name, scenario) ->
            val simulation = SimulationState(scenario)
            val simulator = Simulator(EventLog.noop(), scenario, simulation)
            simulation.beginBatch()
            simulator.runFor(runFor)
            simulation.endBatch()

            name to simulation
        }
        .toImmutableMap()
}
