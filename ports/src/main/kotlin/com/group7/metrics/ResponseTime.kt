package com.group7.metrics

import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.Simulator
import com.group7.properties.Container
import com.group7.properties.LossSink
import com.group7.properties.OutputSink
import com.group7.properties.Source
import com.group7.utils.suffix
import kotlin.time.DurationUnit
import kotlin.time.Instant

/** Time from when an object enters a node to when that object leaves */
sealed class ResponseTime(private val unit: DurationUnit) : InstantaneousMetric() {
    // Map of objects currently in the node to their entry times
    private val entryTimes = mutableMapOf<Any?, Instant>()

    protected abstract fun alreadyEntered(obj: Any?): String

    protected abstract fun neverEntered(obj: Any?): String

    context(sim: Simulator)
    protected fun notifyEnter(obj: Any?) {
        // When objects enter update our track of who's in the node
        val existing = entryTimes.put(obj, contextOf<Simulator>().currentTime)
        check(existing == null) { alreadyEntered(obj) }
        entryTimes[obj] = contextOf<Simulator>().currentTime
    }

    context(sim: Simulator)
    protected fun notifyLeave(obj: Any?) {
        // Remove objects from the map when they leave
        val entryTime = entryTimes.remove(obj)
        check(entryTime != null) { neverEntered(obj) }

        // Immediately notify this as a value
        val currentTime = contextOf<Simulator>().currentTime
        notify(currentTime, (currentTime - entryTime).toDouble(unit))
    }

    // Only used for Global implementation
    protected fun notifyLost(obj: Any?) {
        val entryTime = entryTimes.remove(obj)
        check(entryTime != null) { neverEntered(obj) }
    }

    class Local(private val container: Container<*>, unit: DurationUnit = DurationUnit.SECONDS) : ResponseTime(unit) {
        init {
            container.onEnter { notifyEnter(it) }
            container.onLeave { notifyLeave(it) }
        }

        override fun alreadyEntered(obj: Any?) =
            "Object $obj already entered $container! Make sure to use unique objects to allow calculating response time"

        override fun neverEntered(obj: Any?) = "Object $obj never entered $container!"
    }

    // Global logic reports the same numbers as residence time
    class Global(scenario: Scenario, unit: DurationUnit = DurationUnit.SECONDS) : ResponseTime(unit) {
        init {
            for (source in scenario.allNodeGroups.asSequence().filterIsInstance<Source<*>>()) {
                source.onEmit { notifyEnter(it) }
            }

            for (sink in scenario.allNodeGroups.asSequence().filterIsInstance<OutputSink<*>>()) {
                sink.onEnter { notifyLeave(it) }
            }

            for (sink in scenario.allNodeGroups.asSequence().filterIsInstance<LossSink<*>>()) {
                sink.onEnter { notifyLost(it) }
            }
        }

        override fun alreadyEntered(obj: Any?) =
            "Object $obj was already emitted by a source! Make sure to use unique objects to allow calculating response time"

        override fun neverEntered(obj: Any?) = "Object $obj entered a sink but was never emitted by a source!"
    }

    /** Time from when an object enters a container to when it leaves, or entering the simulation to when it leaves. */
    companion object : MetricFactory<Container<*>>, GlobalMetricFactory {
        override fun create(node: Container<*>, scenario: Scenario) = create(node, DurationUnit.SECONDS)

        fun create(node: Container<*>, unit: DurationUnit): MetricGroup? {
            if (!node.supportsResidenceTime()) {
                return null
            }
            val raw = Local(node, unit)
            val cis = InstantaneousConfidenceIntervals(raw)
            return MetricGroup("Response Time (${unit.suffix})", node as NodeGroup, raw, cis.moments())
        }

        override fun create(scenario: Scenario) = create(scenario, DurationUnit.SECONDS)

        fun create(scenario: Scenario, unit: DurationUnit): MetricGroup {
            val raw = Global(scenario, unit)
            val cis = InstantaneousConfidenceIntervals(raw)
            return MetricGroup("Response Time (${unit.suffix})", null, raw, cis.moments())
        }
    }
}
