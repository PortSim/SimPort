package com.group7.metrics

import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.Simulator
import com.group7.metrics.confidence.InstantaneousConfidenceIntervals
import com.group7.properties.Container
import com.group7.properties.LossSink
import com.group7.properties.OutputSink
import com.group7.properties.Source
import com.group7.utils.suffix
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant

/** Total time each object spends in a node metric, instantaneous metric */
sealed class ResidenceTime(scenario: Scenario, private val unit: DurationUnit) : InstantaneousMetric() {
    // A map of objects currently in the node to their entry times
    private val entryTimes = mutableMapOf<Any?, Instant>()
    // A map of objects to durations so far
    private val totalDurations = mutableMapOf<Any?, Duration>()

    init {
        // Notice when objects disappear into non-output sinks
        for (sink in scenario.every<LossSink<*>>()) {
            sink.onEnter { notifyLost(it) }
        }
    }

    // It's bad when the same object enters a node twice before leaving
    protected abstract fun alreadyEntered(obj: Any?): String

    // It's bad when an object leaves a node before entering
    protected abstract fun neverEntered(obj: Any?): String

    context(sim: Simulator)
    protected fun notifyEnter(obj: Any?) {
        // When objects enter update our track of who's in the node
        val existing = entryTimes.put(obj, sim.currentTime)
        check(existing == null) { alreadyEntered(obj) }
    }

    context(sim: Simulator)
    protected fun notifyLeave(obj: Any?) {
        // Remove objects from the map when they leave
        val entryTime = entryTimes.remove(obj)
        check(entryTime != null) { neverEntered(obj) }

        val currentTime = contextOf<Simulator>().currentTime
        // Add the time in the object and the big map our add the new time if it has already been here
        totalDurations.compute(obj) { _, existing -> (existing ?: Duration.ZERO) + (currentTime - entryTime) }
    }

    context(sim: Simulator)
    protected fun notifyLeaveSimulation(obj: Any?) {
        // When any object leaves the simulation we can report a value, zero if it was never in the node
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

            for (sink in scenario.every<OutputSink<*>>()) {
                sink.onEnter { notifyLeaveSimulation(it) }
            }
        }

        override fun alreadyEntered(obj: Any?) =
            "Object $obj already entered $container! Make sure to use unique objects to allow calculating residence time"

        override fun neverEntered(obj: Any?) = "Object $obj never entered $container!"
    }

    // Global counts total time across all nodes from source -> output sink
    class Global(scenario: Scenario, unit: DurationUnit = DurationUnit.SECONDS) : ResidenceTime(scenario, unit) {
        init {
            for (source in scenario.every<Source<*>>()) {
                source.onEmit { notifyEnter(it) }
            }

            for (sink in scenario.every<OutputSink<*>>()) {
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

    /**
     * Total time each object spends in a container, reported per object. Global reports total time in the simulation.
     *
     * If an object enters a node twice, we add the new duration to the previous one (and then only report this value
     * when thet object leaves the simulation).
     */
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
