package com.group7.metrics

import com.group7.NodeGroup
import com.group7.properties.Container
import kotlin.time.Instant

class Occupancy(private val container: Container<*>) : ContinuousMetric() {
    override fun reportImpl(previousTime: Instant, currentTime: Instant) = container.occupants.toDouble()

    companion object : MetricFactory<Container<*>> {
        override fun createGroup(node: Container<*>): MetricGroup {
            val raw = Occupancy(node)
            val cis = ContinuousConfidenceIntervals(raw)
            return MetricGroup(
                "Occupancy",
                node as NodeGroup,
                raw,
                Moments(cis.mean, cis.lower, cis.upper, cis.variance),
            )
        }
    }
}
