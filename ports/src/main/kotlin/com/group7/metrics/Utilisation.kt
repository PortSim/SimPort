package com.group7.metrics

import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.properties.BoundedContainer
import kotlin.time.Instant

/** Utilisation of a node with an occupancy and capacity, or a service node with a number of objects in service */
sealed class Utilisation : ContinuousMetric() {

    protected abstract val current: Double

    override fun reportImpl(previousTime: Instant, currentTime: Instant) = current

    class Local(private val container: BoundedContainer<*>) : Utilisation() {
        override val current
            // Bounded containers report their utilisation
            get() = container.utilisation
    }

    /**
     * Continuous metric for in-use occupancy of a node / capacity. There is no global version of utilisation.
     *
     * Note: objects occupying nodes but that have finished being serviced should report low utilisation in service
     * nodes. This is how utilisation is supposed to work.
     */
    companion object : MetricFactory<BoundedContainer<*>> {
        override fun create(node: BoundedContainer<*>, scenario: Scenario): MetricGroup {
            val raw = Local(node)
            val cis = ContinuousConfidenceIntervals(raw)
            return MetricGroup("Utilisation", node as NodeGroup, raw, cis.moments())
        }
    }
}
