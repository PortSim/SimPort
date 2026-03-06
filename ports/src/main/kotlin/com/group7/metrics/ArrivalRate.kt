package com.group7.metrics

import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.Simulator
import com.group7.metrics.confidence.RateConfidenceIntervals
import com.group7.metrics.steady.R5Instantaneous
import com.group7.properties.Container
import com.group7.properties.Source
import com.group7.utils.suffix
import kotlin.time.DurationUnit

/** Local and global arrival rate. */
sealed class ArrivalRate(unit: DurationUnit) : RateMetric(unit) {

    context(sim: Simulator)
    protected fun notify() {
        notify(sim.currentTime)
    }

    class Local(container: Container<*>, unit: DurationUnit) : ArrivalRate(unit) {
        init {
            container.onEnter { notify() }
        }
    }

    class Global(scenario: Scenario, unit: DurationUnit) : ArrivalRate(unit) {
        init {
            for (source in scenario.every<Source<*>>()) {
                source.onEmit { notify() }
            }
        }
    }

    /**
     * Rate metric for local and global rate of arrivals.
     *
     * Local arrival rate tracks objects entering a node. Global arrival rate tracks objects leaving source nodes.
     */
    companion object : MetricFactory<Container<*>>, GlobalMetricFactory {
        override fun create(node: Container<*>, scenario: Scenario): MetricGroup = create(node, DurationUnit.HOURS)

        fun create(node: Container<*>, unit: DurationUnit): MetricGroup {
            val raw = Local(node, unit)
            val cis = RateConfidenceIntervals(raw, R5Instantaneous(InterArrivalTime.Local(node, unit)))
            // Do not record raw values for a rate metric
            return MetricGroup("Arrival rate (objects / ${unit.suffix})", node as NodeGroup, null, cis.moments())
        }

        override fun create(scenario: Scenario): MetricGroup = create(scenario, DurationUnit.HOURS)

        fun create(scenario: Scenario, unit: DurationUnit): MetricGroup {
            val raw = Global(scenario, unit)
            val cis = RateConfidenceIntervals(raw, R5Instantaneous(InterArrivalTime.Global(scenario, unit)))
            // Do not record raw values for a rate metric
            return MetricGroup("Arrival rate (objects / ${unit.suffix})", null, null, cis.moments())
        }
    }
}
