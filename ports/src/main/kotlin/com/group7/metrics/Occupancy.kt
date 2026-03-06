package com.group7.metrics

import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.metrics.confidence.ContinuousConfidenceIntervals
import com.group7.properties.Container
import com.group7.properties.Sink
import com.group7.properties.Source
import kotlin.time.Instant

/** Occupancy of nodes continuous metric */
sealed class Occupancy : ContinuousMetric() {

    protected abstract val current: Int

    override fun reportImpl(previousTime: Instant, currentTime: Instant) = current.toDouble()

    /** Local occupancy metric tracking the current number of objects in a single container. */
    class Local(private val container: Container<*>) : Occupancy() {
        // All containers reocrd their occupancy so just get it from there
        override val current
            get() = container.occupants
    }

    /** Global occupancy metric tracking the total number of objects currently in the entire simulation. */
    class Global(scenario: Scenario) : Occupancy() {
        override var current = 0
            private set

        init {
            // Whenever something is emitted from a source increment global occupancy
            for (source in scenario.every<Source<*>>()) {
                source.onEmit { current++ }
            }

            // And then when anything leaves decrement occupancy
            for (sink in scenario.every<Sink<*>>()) {
                sink.onEnter { current-- }
            }
        }
    }

    /**
     * Continuous metric for local and global occupancy.
     *
     * Local occupancy reports for a container. Global occupancy uses sources and sinks to keep track of how many
     * objects are in the system overall.
     */
    companion object : MetricFactory<Container<*>>, GlobalMetricFactory {
        /**
         * Creates a local occupancy metric for a specific node.
         *
         * @param node the container to track occupancy for
         * @param scenario the scenario containing all nodes
         * @return a metric group containing occupancy and its statistical moments
         */
        override fun create(node: Container<*>, scenario: Scenario): MetricGroup {
            val raw = Local(node)
            val cis = ContinuousConfidenceIntervals(raw)
            return MetricGroup("Occupancy", node as NodeGroup, raw, cis.moments())
        }

        /**
         * Creates a global occupancy metric across the entire scenario.
         *
         * @param scenario the scenario to create a global metric for
         * @return a metric group containing global occupancy and its statistical moments
         */
        override fun create(scenario: Scenario): MetricGroup {
            val raw = Global(scenario)
            val cis = ContinuousConfidenceIntervals(raw)
            return MetricGroup("Occupancy", null, raw, cis.moments())
        }
    }
}
