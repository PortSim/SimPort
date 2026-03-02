package com.group7.metrics

import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.properties.BoundedContainer
import kotlin.time.Instant

sealed class Utilisation : ContinuousMetric() {

    protected abstract val current: Double

    override fun reportImpl(previousTime: Instant, currentTime: Instant) = current

    class Local(private val container: BoundedContainer<*>) : Utilisation() {
        override val current
            get() = container.utilisation
    }

    companion object : MetricFactory<BoundedContainer<*>> {
        override fun create(node: BoundedContainer<*>, scenario: Scenario): MetricGroup {
            val raw = Local(node)
            val cis = ContinuousConfidenceIntervals(raw)
            return MetricGroup("Utilisation", node as NodeGroup, raw, cis.moments())
        }
    }
}
