package com.group7.metrics

import com.group7.NodeGroup
import com.group7.Simulator
import com.group7.properties.Service
import kotlin.time.DurationUnit
import kotlin.time.Instant

class ServiceTime(service: Service<*>, private val unit: DurationUnit = DurationUnit.SECONDS) : InstantaneousMetric() {
    private lateinit var enterTime: Instant

    init {
        service.onEnter { enterTime = contextOf<Simulator>().currentTime }
        service.onLeave {
            val currentTime = contextOf<Simulator>().currentTime
            notify(currentTime, (currentTime - enterTime).toDouble(unit))
        }
    }

    companion object : MetricFactory<Service<*>> {
        override fun createGroup(node: Service<*>) = createGroup(node, unit = DurationUnit.SECONDS)

        fun createGroup(node: Service<*>, unit: DurationUnit = DurationUnit.SECONDS): MetricGroup {
            val raw = ServiceTime(node, unit)
            val cis = InstantaneousConfidenceIntervals(raw)
            return MetricGroup(
                "Service Time (${unit.suffix})",
                node as NodeGroup,
                raw,
                Moments(cis.mean, cis.lower, cis.upper, cis.variance),
            )
        }
    }
}

private val DurationUnit.suffix
    get() =
        when (this) {
            DurationUnit.NANOSECONDS -> "ns"
            DurationUnit.MICROSECONDS -> "µs"
            DurationUnit.MILLISECONDS -> "ms"
            DurationUnit.SECONDS -> "s"
            DurationUnit.MINUTES -> "min"
            DurationUnit.HOURS -> "h"
            DurationUnit.DAYS -> "d"
        }
