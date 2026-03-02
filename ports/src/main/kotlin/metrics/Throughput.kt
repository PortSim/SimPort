package com.group7.metrics

import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.Simulator
import com.group7.properties.Container
import com.group7.properties.Sink
import com.group7.utils.suffix
import kotlin.time.DurationUnit

sealed class Throughput(unit: DurationUnit) : RateMetric(unit) {

    context(sim: Simulator)
    protected fun notify() {
        notify(sim.currentTime)
    }

    class Local(container: Container<*>, unit: DurationUnit) : Throughput(unit) {
        init {
            container.onLeave { notify() }
        }
    }

    class Global(scenario: Scenario, unit: DurationUnit) : Throughput(unit) {
        init {
            for (sink in scenario.allNodes.asSequence().filterIsInstance<Sink<*>>()) {
                sink.onEnter { notify() }
            }
        }
    }

    companion object : MetricFactory<Container<*>>, GlobalMetricFactory {
        override fun create(node: Container<*>, scenario: Scenario): MetricGroup = create(node, DurationUnit.HOURS)

        fun create(node: Container<*>, unit: DurationUnit): MetricGroup {
            val raw = Local(node, unit)
            val cis = RateConfidenceIntervals(raw, R5Instantaneous(InterDepartureTime.Local(node, unit)))
            return MetricGroup("Throughput (objects / ${unit.suffix})", node as NodeGroup, null, cis.moments())
        }

        override fun create(scenario: Scenario): MetricGroup = create(scenario, DurationUnit.HOURS)

        fun create(scenario: Scenario, unit: DurationUnit): MetricGroup {
            val raw = Global(scenario, unit)
            val cis = RateConfidenceIntervals(raw, R5Instantaneous(InterDepartureTime.Global(scenario, unit)))
            return MetricGroup("Throughput (objects / ${unit.suffix})", null, null, cis.moments())
        }
    }
}
