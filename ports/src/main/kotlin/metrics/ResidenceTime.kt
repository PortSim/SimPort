package com.group7.metrics

import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.Simulator
import com.group7.properties.Container
import com.group7.properties.LossSink
import com.group7.properties.OutputSink
import com.group7.properties.Source
import com.group7.utils.suffix
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant

sealed class ResidenceTime(scenario: Scenario, private val unit: DurationUnit) : InstantaneousMetric() {
    private val entryTimes = mutableMapOf<Any?, Instant>()
    private val totalDurations = mutableMapOf<Any?, Duration>()

    init {
        for (sink in scenario.allNodes.asSequence().filterIsInstance<LossSink<*>>()) {
            sink.onEnter { notifyLost(it) }
        }
    }

    protected abstract fun alreadyEntered(obj: Any?): String

    protected abstract fun neverEntered(obj: Any?): String

    context(sim: Simulator)
    protected fun notifyEnter(obj: Any?) {
        val existing = entryTimes.put(obj, sim.currentTime)
        check(existing == null) { alreadyEntered(obj) }
    }

    context(sim: Simulator)
    protected fun notifyLeave(obj: Any?) {
        val entryTime = entryTimes.remove(obj)
        check(entryTime != null) { neverEntered(obj) }

        val currentTime = contextOf<Simulator>().currentTime
        totalDurations.compute(obj) { _, existing -> (existing ?: Duration.ZERO) + (currentTime - entryTime) }
    }

    context(sim: Simulator)
    protected fun notifyLeaveSimulation(obj: Any?) {
        val totalDuration = totalDurations.remove(obj) ?: Duration.ZERO
        notify(sim.currentTime, totalDuration.toDouble(unit))
    }

    private fun notifyLost(obj: Any?) {
        entryTimes.remove(obj)
        totalDurations.remove(obj)
    }

    class Local(private val container: Container<*>, scenario: Scenario, unit: DurationUnit = DurationUnit.SECONDS) :
        ResidenceTime(scenario, unit) {
        init {
            container.onEnter { notifyEnter(it) }
            container.onLeave { notifyLeave(it) }

            for (sink in scenario.allNodes.asSequence().filterIsInstance<OutputSink<*>>()) {
                sink.onEnter { notifyLeaveSimulation(it) }
            }
        }

        override fun alreadyEntered(obj: Any?) =
            "Object $obj already entered $container! Make sure to use unique objects to allow calculating residence time"

        override fun neverEntered(obj: Any?) = "Object $obj never entered $container!"
    }

    class Global(scenario: Scenario, unit: DurationUnit = DurationUnit.SECONDS) : ResidenceTime(scenario, unit) {
        init {
            for (source in scenario.allNodes.asSequence().filterIsInstance<Source<*>>()) {
                source.onEmit { notifyEnter(it) }
            }

            for (sink in scenario.allNodes.asSequence().filterIsInstance<OutputSink<*>>()) {
                sink.onEnter {
                    notifyLeave(it)
                    notifyLeaveSimulation(it)
                }
            }
        }

        override fun alreadyEntered(obj: Any?) =
            "Object $obj was already emitted by a source! Make sure to use unique objects to allow calculating residence time"

        override fun neverEntered(obj: Any?) = "Object $obj entered a sink but was never emitted by a source!"
    }

    companion object : MetricFactory<Container<*>>, GlobalMetricFactory {
        override fun create(node: Container<*>, scenario: Scenario) = create(node, scenario, DurationUnit.SECONDS)

        fun create(node: Container<*>, scenario: Scenario, unit: DurationUnit): MetricGroup? {
            if (!node.supportsResidenceTime()) {
                return null
            }
            val raw = Local(node, scenario, unit)
            val cis = InstantaneousConfidenceIntervals(raw)
            return MetricGroup("Residence Time (${unit.suffix})", node as NodeGroup, raw, cis.moments())
        }

        override fun create(scenario: Scenario) = create(scenario, DurationUnit.SECONDS)

        fun create(scenario: Scenario, unit: DurationUnit): MetricGroup {
            val raw = Global(scenario, unit)
            val cis = InstantaneousConfidenceIntervals(raw)
            return MetricGroup("Residence Time (${unit.suffix})", null, raw, cis.moments())
        }
    }
}
