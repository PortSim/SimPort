package com.group7.metrics

import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.Simulator
import com.group7.metrics.confidence.RateConfidenceIntervals
import com.group7.metrics.steady.R5Instantaneous
import com.group7.properties.Container
import com.group7.properties.OutputSink
import com.group7.utils.suffix
import kotlin.time.DurationUnit

/** Rate of departures from a node, rate metric */
sealed class Throughput(unit: DurationUnit) : RateMetric(unit) {

    context(sim: Simulator)
    protected fun notify() {
        notify(sim.currentTime)
    }

    /** Local throughput metric tracking rate of objects departing from a single node. */
    class Local(container: Container<*>, unit: DurationUnit) : Throughput(unit) {
        init {
            // Local events are when objects leave the container
            container.onLeave { notify() }
        }
    }

    /** Global throughput metric tracking rate of objects entering any output sink. */
    class Global(scenario: Scenario, unit: DurationUnit) : Throughput(unit) {
        init {
            // Global events are when objects enter output sinks
            for (sink in scenario.every<OutputSink<*>>()) {
                sink.onEnter { notify() }
            }
        }
    }

    /**
     * Rate metric for local and global rate of departures.
     *
     * Local arrival rate tracks objects leaving a node. Global arrival rate tracks objects entering output sink nodes.
     */
    companion object : MetricFactory<Container<*>>, GlobalMetricFactory {
        /**
         * Creates a throughput metric for a specific node (uses default time unit of hours).
         *
         * @param node the container to track throughput for
         * @param scenario the scenario containing all nodes
         * @return a metric group containing the throughput and its statistical moments
         */
        override fun create(node: Container<*>, scenario: Scenario): MetricGroup = create(node, DurationUnit.HOURS)

        /**
         * Creates a throughput metric for a specific node with optional time unit.
         *
         * @param node the container to track throughput for
         * @param unit the time unit for the rate (default: hours)
         * @return a metric group containing the throughput and its statistical moments
         */
        fun create(node: Container<*>, unit: DurationUnit): MetricGroup {
            val raw = Local(node, unit)
            val cis = RateConfidenceIntervals(raw, R5Instantaneous(InterDepartureTime.Local(node, unit)))
            return MetricGroup("Throughput (objects / ${unit.suffix})", node as NodeGroup, null, cis.moments())
        }

        /**
         * Creates a global throughput metric across the entire scenario (uses default time unit of hours).
         *
         * @param scenario the scenario to create a global metric for
         * @return a metric group containing the global throughput and its statistical moments
         */
        override fun create(scenario: Scenario): MetricGroup = create(scenario, DurationUnit.HOURS)

        /**
         * Creates a global throughput metric across the entire scenario with optional time unit.
         *
         * @param scenario the scenario to create a global metric for
         * @param unit the time unit for the rate (default: hours)
         * @return a metric group containing the global throughput and its statistical moments
         */
        fun create(scenario: Scenario, unit: DurationUnit): MetricGroup {
            val raw = Global(scenario, unit)
            val cis = RateConfidenceIntervals(raw, R5Instantaneous(InterDepartureTime.Global(scenario, unit)))
            return MetricGroup("Throughput (objects / ${unit.suffix})", null, null, cis.moments())
        }
    }
}
