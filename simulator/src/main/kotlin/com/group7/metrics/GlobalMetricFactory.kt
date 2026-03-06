package com.group7.metrics

import com.group7.Scenario

/**
 * Factory for creating global metrics across the entire simulation.
 *
 * Unlike [MetricFactory] which creates metrics for individual nodes, global metric factories create metrics that span
 * the entire [Scenario].
 */
fun interface GlobalMetricFactory {
    /**
     * Creates a global metric for the entire scenario.
     *
     * @param scenario the [Scenario] to create a metric for
     * @return a [MetricGroup] representing a global metric
     */
    fun create(scenario: Scenario): MetricGroup
}
