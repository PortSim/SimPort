package com.group7.metrics

import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.properties.Container
import com.group7.properties.Sink
import com.group7.properties.Source
import kotlin.time.Instant

sealed class Occupancy : ContinuousMetric() {

    protected abstract val current: Int

    override fun reportImpl(previousTime: Instant, currentTime: Instant) = current.toDouble()

    class Local(private val container: Container<*>) : Occupancy() {
        override val current
            get() = container.occupants
    }

    class Global(scenario: Scenario) : Occupancy() {
        override var current = 0
            private set

        init {
            for (source in scenario.allNodes.asSequence().filterIsInstance<Source<*>>()) {
                source.onEmit { current++ }
            }

            for (sink in scenario.allNodes.asSequence().filterIsInstance<Sink<*>>()) {
                sink.onEnter { current-- }
            }
        }
    }

    companion object : MetricFactory<Container<*>>, GlobalMetricFactory {
        override fun create(node: Container<*>): MetricGroup {
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
