package com.group7.metrics

import com.group7.MetricReporter
import com.group7.Scenario
import kotlin.time.Instant

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
 * @param scenario the [Scenario] whose metrics are being tracked
 * @param downsample whether to downsample the metrics for performance reasons
 */
class MetricsTracker(scenario: Scenario, downsample: Boolean) : MetricReporter {
    /** All metric groups grouped by name. */
    val metricGroups = scenario.metrics.groupBy { it.name }

    /** All individual metrics from all metric groups. */
    val allMetrics = metricGroups.values.asSequence().flatten().flatMap { it.allMetrics }.toList()

    private val buffers = allMetrics.associateWithTo(mutableMapOf()) { MetricData(it is ContinuousMetric, downsample) }
    private val continuousMetrics =
        allMetrics.asSequence().filterIsInstance<ContinuousMetric>().map { it to buffers.getValue(it) }.toList()

    /**
     * Retrieves all collected data points for a specific metric.
     *
     * @param metric the [Metric] to get data for
     * @return a list of [MetricValue] objects collected for this metric
     */
    fun getMetricDataPoints(currentTime: Instant, metric: Metric): List<MetricValue> {
        val recorded = buffers.getValue(metric).values
        if (metric is ContinuousMetric && recorded.isNotEmpty() && recorded.last().time != currentTime) {
            // End the downsampled sample
            return recorded.add(MetricValue(currentTime, recorded.last().value))
        }
        return recorded
    }

    init {
        for (metric in allMetrics) {
            if (metric is InstantaneousMetric) {
                val buffer = buffers.getValue(metric)
                metric.onFire { currentTime, value ->
                    if (!value.isNaN()) {
                        buffer.add(currentTime, value)
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
        for ((metric, buffer) in continuousMetrics) {
            val value = metric.report(currentTime)
            if (value.isNaN()) {
                continue
            }
            buffer.add(currentTime, value)
        }
    }
}
