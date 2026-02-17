package com.group7.metrics

import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.Simulator
import com.group7.properties.Container
import com.group7.properties.Sink
import com.group7.utils.suffix
import kotlin.time.DurationUnit
import kotlin.time.Instant

sealed class Latency(private val unit: DurationUnit) : InstantaneousMetric() {
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

    class Local(container: Container<*>, unit: DurationUnit = DurationUnit.SECONDS) : Latency(unit) {
        init {
            container.onEnter { notifySeen() }
        }
    }

    class Global(scenario: Scenario, unit: DurationUnit = DurationUnit.SECONDS) : Latency(unit) {
        init {
            for (sink in scenario.allNodes.asSequence().filterIsInstance<Sink<*>>()) {
                sink.onEnter { notifySeen() }
            }
        }
    }

    companion object : MetricFactory<Container<*>>, GlobalMetricFactory {
        override fun create(node: Container<*>) = create(node, DurationUnit.SECONDS)

        fun create(node: Container<*>, unit: DurationUnit): MetricGroup? {
            if (!node.supportsResidenceTime()) {
                return null
            }
            val raw = Local(node, unit)
            val cis = InstantaneousConfidenceIntervals(raw)
            return MetricGroup("Latency (${unit.suffix})", node as NodeGroup, raw, cis.moments())
        }

        override fun create(scenario: Scenario) = create(scenario, DurationUnit.SECONDS)

        fun create(scenario: Scenario, unit: DurationUnit): MetricGroup {
            val raw = Global(scenario, unit)
            val cis = InstantaneousConfidenceIntervals(raw)
            return MetricGroup("Latency (${unit.suffix})", null, raw, cis.moments())
        }
    }
}
