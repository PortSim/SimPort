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

    class Local(private val container: Container<*>) : Occupancy() {
        // All containers reocrd their occupancy so just get it from there
        override val current
            get() = container.occupants
    }

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
        override fun create(node: Container<*>, scenario: Scenario): MetricGroup {
            val raw = Local(node)
            val cis = ContinuousConfidenceIntervals(raw)
            return MetricGroup("Occupancy", node as NodeGroup, raw, cis.moments())
        }

        override fun create(scenario: Scenario): MetricGroup {
            val raw = Global(scenario)
            val cis = ContinuousConfidenceIntervals(raw)
            return MetricGroup("Occupancy", null, raw, cis.moments())
        }
    }
}
