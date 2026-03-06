package com.group7.metrics

import com.group7.MetricReporter
import com.group7.Scenario
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf

/**
 * A recorded value of a metric at a specific simulation time.
 *
 * @property time the simulation time at which this value was recorded
 * @property value the numeric value of the metric
 */
data class MetricValue(val time: Instant, val value: Double)

/**
 * Collects and buffers metric values during simulation execution.
 *
 * The metrics tracker subscribes to all metrics in a scenario and collects their values as the simulation runs.
 * Collected data can be queried after simulation completes.
 *
 * @property scenario the [Scenario] whose metrics are being tracked
 */
class MetricsTracker(scenario: Scenario) : MetricReporter {
    /** All metric groups grouped by name. */
    val metricGroups = scenario.metrics.groupBy { it.name }

    /** All individual metrics from all metric groups. */
    val allMetrics = metricGroups.values.asSequence().flatten().flatMap { it.allMetrics }.toList()

    private val continuousMetrics = allMetrics.filterIsInstance<ContinuousMetric>()
    private val buffer = allMetrics.associateWithTo(mutableMapOf()) { persistentListOf<MetricValue>() }

    /**
     * Retrieves all collected data points for a specific metric.
     *
     * @param metric the [Metric] to get data for
     * @return a list of [MetricValue] objects collected for this metric
     */
    fun getMetricDataPoints(metric: Metric) = buffer.getValue(metric)

    init {
        for (metric in allMetrics) {
            if (metric is InstantaneousMetric) {
                metric.onFire { currentTime, value ->
                    if (!value.isNaN()) {
                        buffer.computeIfPresent(metric) { _, list -> list.add(MetricValue(currentTime, value)) }
                    }
                }
            }
        }
    }

    /**
     * Reports metrics at the current simulation time.
     *
     * Called by the simulator at each time step to collect continuous metric values.
     *
     * @param currentTime the current simulation time
     */
    override fun report(currentTime: Instant) {
        for (metric in continuousMetrics) {
            val value = metric.report(currentTime)
            if (value.isNaN()) {
                continue
            }
            buffer.computeIfPresent(metric) { _, list -> list.add(MetricValue(currentTime, value)) }
        }
    }
}
