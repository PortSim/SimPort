package com.group7.metrics

import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.Simulator
import com.group7.metrics.confidence.InstantaneousConfidenceIntervals
import com.group7.properties.Container
import com.group7.properties.OutputSink
import com.group7.utils.suffix
import kotlin.time.DurationUnit
import kotlin.time.Instant

/** Time between departures instantaneous metric */
sealed class InterDepartureTime(private val unit: DurationUnit) : InstantaneousMetric() {
    private var lastSeen: Instant? = null

    context(sim: Simulator)
    protected fun notifySeen() {
        val currentTime = sim.currentTime
        val lastSeen = lastSeen
        if (lastSeen != null) {
            notify(currentTime, (currentTime - lastSeen).toDouble(unit))
        }
        this.lastSeen = currentTime
    }

    /** Local inter-departure time metric measuring gaps between successive departures from a node. */
    class Local(container: Container<*>, unit: DurationUnit = DurationUnit.SECONDS) : InterDepartureTime(unit) {
        init {
            // Notify when any object leaves the container
            container.onLeave { notifySeen() }
        }
    }

    /** Global inter-departure time metric measuring gaps between successive arrivals at any output sink. */
    class Global(scenario: Scenario, unit: DurationUnit = DurationUnit.SECONDS) : InterDepartureTime(unit) {
        init {
            // Notify when any object entering an output sink
            for (sink in scenario.every<OutputSink<*>>()) {
                sink.onEnter { notifySeen() }
            }
        }
    }

    /**
     * Instantaneous metric for time between departures, instantaneous metric.
     *
     * Local inter-departure time triggered by objects leaving nodes. Global inter-arrival time based on gaps between
     * arrivals at any sink nodes.
     */
    companion object : MetricFactory<Container<*>>, GlobalMetricFactory {
        /**
         * Creates an inter-departure time metric for a specific node (uses default time unit of seconds).
         *
         * @param node the container to track inter-departure time for
         * @param scenario the scenario containing all nodes
         * @return a metric group containing inter-departure times and statistical moments, or null if node doesn't support residence time
         */
        override fun create(node: Container<*>, scenario: Scenario) = create(node, DurationUnit.SECONDS)

        /**
         * Creates an inter-departure time metric for a specific node with optional time unit.
         *
         * @param node the container to track inter-departure time for
         * @param unit the time unit for intervals (default: seconds)
         * @return a metric group containing inter-departure times and statistical moments, or null if node doesn't support residence time
         */
        fun create(node: Container<*>, unit: DurationUnit): MetricGroup? {
            if (!node.supportsResidenceTime()) {
                return null
            }
            val raw = Local(node, unit)
            val cis = InstantaneousConfidenceIntervals(raw)
            return MetricGroup("Inter-departure time (${unit.suffix})", node as NodeGroup, raw, cis.moments())
        }

        /**
         * Creates a global inter-departure time metric across the entire scenario (uses default time unit of seconds).
         *
         * @param scenario the scenario to create a global metric for
         * @return a metric group containing global inter-departure times and statistical moments
         */
        override fun create(scenario: Scenario) = create(scenario, DurationUnit.SECONDS)

        /**
         * Creates a global inter-departure time metric across the entire scenario with optional time unit.
         *
         * @param scenario the scenario to create a global metric for
         * @param unit the time unit for intervals (default: seconds)
         * @return a metric group containing global inter-departure times and statistical moments
         */
        fun create(scenario: Scenario, unit: DurationUnit): MetricGroup {
            val raw = Global(scenario, unit)
            val cis = InstantaneousConfidenceIntervals(raw)
            return MetricGroup("Inter-departure time (${unit.suffix})", null, raw, cis.moments())
        }
    }
}
