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

    /** Local response time metric tracking time from entry to exit of a single container. */
    class Local(private val container: Container<*>, unit: DurationUnit = DurationUnit.SECONDS) : ResponseTime(unit) {
        init {
            container.onEnter { notifyEnter(it) }
            container.onLeave { notifyLeave(it) }
        }

        override fun alreadyEntered(obj: Any?) =
            "Object $obj already entered $container! Make sure to use unique objects to allow calculating response time"

        override fun neverEntered(obj: Any?) = "Object $obj never entered $container!"
    }

    /** Global response time metric tracking time from entering simulation to exiting at a sink. */
    class Global(scenario: Scenario, unit: DurationUnit = DurationUnit.SECONDS) : ResponseTime(unit) {
        init {
            for (source in scenario.every<Source<*>>()) {
                source.onEmit { notifyEnter(it) }
            }

            for (sink in scenario.every<OutputSink<*>>()) {
                sink.onEnter { notifyLeave(it) }
            }

            for (sink in scenario.every<LossSink<*>>()) {
                sink.onEnter { notifyLost(it) }
            }
        }

        override fun alreadyEntered(obj: Any?) =
            "Object $obj was already emitted by a source! Make sure to use unique objects to allow calculating response time"

        override fun neverEntered(obj: Any?) = "Object $obj entered a sink but was never emitted by a source!"
    }

    /** Time from when an object enters a container to when it leaves, or entering the simulation to when it leaves. */
    companion object : MetricFactory<Container<*>>, GlobalMetricFactory {
        /**
         * Creates a response time metric for a specific node (uses default time unit of seconds).
         *
         * @param node the container to track response time for
         * @param scenario the scenario containing all nodes
         * @return a metric group containing response times and statistical moments, or null if node doesn't support
         *   residence time
         */
        override fun create(node: Container<*>, scenario: Scenario) = create(node, DurationUnit.SECONDS)

        /**
         * Creates a response time metric for a specific node with optional time unit.
         *
         * @param node the container to track response time for
         * @param unit the time unit for durations (default: seconds)
         * @return a metric group containing response times and statistical moments, or null if node doesn't support
         *   residence time
         */
        fun create(node: Container<*>, unit: DurationUnit): MetricGroup? {
            if (!node.supportsResidenceTime()) {
                return null
            }
            val raw = Local(node, unit)
            val cis = InstantaneousConfidenceIntervals(raw)
            return MetricGroup("Response Time (${unit.suffix})", node as NodeGroup, raw, cis.moments())
        }

        /**
         * Creates a global response time metric across the entire scenario (uses default time unit of seconds).
         *
         * @param scenario the scenario to create a global metric for
         * @return a metric group containing global response times and statistical moments
         */
        override fun create(scenario: Scenario) = create(scenario, DurationUnit.SECONDS)

        /**
         * Creates a global response time metric across the entire scenario with optional time unit.
         *
         * @param scenario the scenario to create a global metric for
         * @param unit the time unit for durations (default: seconds)
         * @return a metric group containing global response times and statistical moments
         */
        fun create(scenario: Scenario, unit: DurationUnit): MetricGroup {
            val raw = Global(scenario, unit)
            val cis = InstantaneousConfidenceIntervals(raw)
            return MetricGroup("Response Time (${unit.suffix})", null, raw, cis.moments())
        }
    }
}
