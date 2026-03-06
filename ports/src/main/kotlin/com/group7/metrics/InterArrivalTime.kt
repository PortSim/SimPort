package com.group7.metrics

import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.Simulator
import com.group7.metrics.confidence.InstantaneousConfidenceIntervals
import com.group7.properties.Container
import com.group7.properties.Source
import com.group7.utils.suffix
import kotlin.time.DurationUnit
import kotlin.time.Instant

/** Time between arrivals instantaneous metric */
sealed class InterArrivalTime(private val unit: DurationUnit) : InstantaneousMetric() {
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

    /** Local inter-arrival time metric measuring gaps between successive arrivals at a node. */
    class Local(container: Container<*>, unit: DurationUnit = DurationUnit.SECONDS) : InterArrivalTime(unit) {
        init {
            // Notify when any object enters the container
            container.onEnter { notifySeen() }
        }
    }

    /** Global inter-arrival time metric measuring gaps between successive departures from any source. */
    class Global(scenario: Scenario, unit: DurationUnit = DurationUnit.SECONDS) : InterArrivalTime(unit) {
        init {
            // Notify when any object leaves a source
            for (source in scenario.every<Source<*>>()) {
                source.onEmit { notifySeen() }
            }
        }
    }

    /**
     * Instantaneous metric for time between arrivals instantaneous metric.
     *
     * Local inter-arrival time triggered by objects entering nodes. Global inter-arrival time based on gaps between
     * departures at any source nodes.
     */
    companion object : MetricFactory<Container<*>>, GlobalMetricFactory {
        /**
         * Creates an inter-arrival time metric for a specific node (uses default time unit of seconds).
         *
         * @param node the container to track inter-arrival time for
         * @param scenario the scenario containing all nodes
         * @return a metric group containing inter-arrival times and statistical moments, or null if node doesn't
         *   support residence time
         */
        override fun create(node: Container<*>, scenario: Scenario) = create(node, DurationUnit.SECONDS)

        /**
         * Creates an inter-arrival time metric for a specific node with optional time unit.
         *
         * @param node the container to track inter-arrival time for
         * @param unit the time unit for intervals (default: seconds)
         * @return a metric group containing inter-arrival times and statistical moments, or null if node doesn't
         *   support residence time
         */
        fun create(node: Container<*>, unit: DurationUnit): MetricGroup? {
            if (!node.supportsResidenceTime()) {
                return null
            }
            val raw = Local(node, unit)
            val cis = InstantaneousConfidenceIntervals(raw)
            return MetricGroup("Inter-arrival time (${unit.suffix})", node as NodeGroup, raw, cis.moments())
        }

        /**
         * Creates a global inter-arrival time metric across the entire scenario (uses default time unit of seconds).
         *
         * @param scenario the scenario to create a global metric for
         * @return a metric group containing global inter-arrival times and statistical moments
         */
        override fun create(scenario: Scenario) = create(scenario, DurationUnit.SECONDS)

        /**
         * Creates a global inter-arrival time metric across the entire scenario with optional time unit.
         *
         * @param scenario the scenario to create a global metric for
         * @param unit the time unit for intervals (default: seconds)
         * @return a metric group containing global inter-arrival times and statistical moments
         */
        fun create(scenario: Scenario, unit: DurationUnit): MetricGroup {
            val raw = Global(scenario, unit)
            val cis = InstantaneousConfidenceIntervals(raw)
            return MetricGroup("Inter-arrival time (${unit.suffix})", null, raw, cis.moments())
        }
    }
}
