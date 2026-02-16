package com.group7.metrics

import com.group7.Scenario
import com.group7.Simulator
import com.group7.properties.Sink
import com.group7.properties.Source
import com.group7.utils.suffix
import kotlin.time.DurationUnit
import kotlin.time.Instant

/** Measures the overall time spent in the system from source to sink */
class SojournTime(scenario: Scenario, unit: DurationUnit = DurationUnit.SECONDS) : InstantaneousMetric() {
    private val startTimes = mutableMapOf<Any?, Instant>()

    init {
        for (source in scenario.allNodes.asSequence().filterIsInstance<Source<*>>()) {
            source.onEmit { obj ->
                val existing = startTimes.put(obj, contextOf<Simulator>().currentTime)
                check(existing == null) {
                    "Object $obj was already emitted by $source! Make sure to use unique objects to allow calculating sojourn time"
                }
                startTimes[obj] = contextOf<Simulator>().currentTime
            }
        }

        for (sink in scenario.allNodes.asSequence().filterIsInstance<Sink<*>>()) {
            sink.onEnter { obj ->
                val startTime = startTimes.remove(obj)
                check(startTime != null) { "Object $obj entered $sink but was never emitted by a source!" }

                val currentTime = contextOf<Simulator>().currentTime
                notify(currentTime, (currentTime - startTime).toDouble(unit))
            }
        }
    }

    companion object : GlobalMetricFactory {
        override fun createGroup(scenario: Scenario) = createGroup(scenario, unit = DurationUnit.SECONDS)

        fun createGroup(scenario: Scenario, unit: DurationUnit = DurationUnit.SECONDS): MetricGroup {
            val raw = SojournTime(scenario, unit)
            val cis = InstantaneousConfidenceIntervals(raw)
            return MetricGroup(
                "Sojourn Time (${unit.suffix})",
                null,
                raw,
                Moments(cis.mean, cis.lower, cis.upper, cis.variance),
            )
        }
    }
}
