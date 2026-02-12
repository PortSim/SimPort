package demos

import com.group7.*
import components.MetricsPanelState
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

private fun sweep(scenarios: List<Pair<String, Scenario>>): ImmutableMap<String, MetricsPanelState> {
    // Run simulations
    val runFor = 5.days
    return scenarios
        .associate { (name, scenario) ->
            val metricsPanelState = MetricsPanelState(scenario)
            val simulator = Simulator(EventLog.noop(), scenario, metricsPanelState)
            metricsPanelState.beginBatch()
            simulator.runFor(runFor)
            metricsPanelState.endBatch()

            name to metricsPanelState
        }
        .toImmutableMap()
}
