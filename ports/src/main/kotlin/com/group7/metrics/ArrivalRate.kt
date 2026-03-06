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

    /** Local arrival rate metric tracking objects entering a single node. */
    class Local(container: Container<*>, unit: DurationUnit) : ArrivalRate(unit) {
        init {
            container.onEnter { notify() }
        }
    }

    /** Global arrival rate metric tracking objects leaving any source node. */
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
        /**
         * Creates an arrival rate metric for a specific node (uses default time unit of hours).
         *
         * @param node the container to track arrival rate for
         * @param scenario the scenario containing all nodes
         * @return a metric group containing the arrival rate and its statistical moments
         */
        override fun create(node: Container<*>, scenario: Scenario): MetricGroup = create(node, DurationUnit.HOURS)

        /**
         * Creates an arrival rate metric for a specific node with optional time unit.
         *
         * @param node the container to track arrival rate for
         * @param unit the time unit for the rate (default: hours)
         * @return a metric group containing the arrival rate and its statistical moments
         */
        fun create(node: Container<*>, unit: DurationUnit): MetricGroup {
            val raw = Local(node, unit)
            val cis = RateConfidenceIntervals(raw, R5Instantaneous(InterArrivalTime.Local(node, unit)))
            // Do not record raw values for a rate metric
            return MetricGroup("Arrival rate (objects / ${unit.suffix})", node as NodeGroup, null, cis.moments())
        }

        /**
         * Creates a global arrival rate metric across the entire scenario (uses default time unit of hours).
         *
         * @param scenario the scenario to create a global metric for
         * @return a metric group containing the global arrival rate and its statistical moments
         */
        override fun create(scenario: Scenario): MetricGroup = create(scenario, DurationUnit.HOURS)

        /**
         * Creates a global arrival rate metric across the entire scenario with optional time unit.
         *
         * @param scenario the scenario to create a global metric for
         * @param unit the time unit for the rate (default: hours)
         * @return a metric group containing the global arrival rate and its statistical moments
         */
        fun create(scenario: Scenario, unit: DurationUnit): MetricGroup {
            val raw = Global(scenario, unit)
            val cis = RateConfidenceIntervals(raw, R5Instantaneous(InterArrivalTime.Global(scenario, unit)))
            // Do not record raw values for a rate metric
            return MetricGroup("Arrival rate (objects / ${unit.suffix})", null, null, cis.moments())
        }
    }
}
