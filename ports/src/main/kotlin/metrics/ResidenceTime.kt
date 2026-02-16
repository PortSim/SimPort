package com.group7.metrics

import com.group7.NodeGroup
import com.group7.Simulator
import com.group7.properties.Container
import com.group7.utils.suffix
import kotlin.time.DurationUnit
import kotlin.time.Instant

class ResidenceTime(container: Container<*>, private val unit: DurationUnit = DurationUnit.SECONDS) :
    InstantaneousMetric() {
    private val entryTimes = mutableMapOf<Any?, Instant>()

    init {
        container.onEnter { obj ->
            val existing = entryTimes.put(obj, contextOf<Simulator>().currentTime)
            check(existing == null) {
                "Object $obj already entered $container! Make sure to use unique objects to allow calculating residence time"
            }
            entryTimes[obj] = contextOf<Simulator>().currentTime
        }
        container.onLeave { obj ->
            val entryTime = entryTimes.remove(obj)
            check(entryTime != null) { "Object $obj never entered $container!" }

            val currentTime = contextOf<Simulator>().currentTime
            notify(currentTime, (currentTime - entryTime).toDouble(unit))
        }
    }

    companion object : MetricFactory<Container<*>> {
        override fun createGroup(node: Container<*>) = createGroup(node, unit = DurationUnit.SECONDS)

        fun createGroup(node: Container<*>, unit: DurationUnit = DurationUnit.SECONDS): MetricGroup? {
            if (!node.supportsResidenceTime()) {
                return null
            }
            val raw = ResidenceTime(node, unit)
            val cis = InstantaneousConfidenceIntervals(raw)
            return MetricGroup(
                "Residence Time (${unit.suffix})",
                node as NodeGroup,
                raw,
                Moments(cis.mean, cis.lower, cis.upper, cis.variance),
            )
        }
    }
}
