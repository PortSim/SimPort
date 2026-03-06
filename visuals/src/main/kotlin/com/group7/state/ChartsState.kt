package com.group7.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.group7.metrics.Metric
import com.group7.metrics.MetricValue
import com.group7.metrics.MetricsTracker
import kotlin.time.Instant

/**
 * Tracks per-metric time-series data for chart rendering, throttling UI updates so that Compose snapshots are only
 * written every [redrawEveryNSamples] samples.
 */
/**
 * Tracks per-metric time-series data for chart rendering, throttling UI updates so that Compose snapshots are only
 * written every [redrawEveryNSamples] samples.
 */
class ChartsState(private val metricsTracker: MetricsTracker, private val redrawEveryNSamples: Int = 10) {
    /** Per-metric time series data using Compose-observable SnapshotStateLists */
    private val metricData = metricsTracker.allMetrics.associateWith { mutableStateOf(emptyList<MetricValue>()) }
    private var samplesSinceRedraw = 0
    var latestTimeSeen by mutableStateOf(Instant.DISTANT_PAST)
        private set

    fun update(currentTime: Instant, force: Boolean) {
        if (force || ++samplesSinceRedraw >= redrawEveryNSamples) {
            latestTimeSeen = currentTime
            samplesSinceRedraw = 0
            for (metric in metricsTracker.allMetrics) {
                metricData.getValue(metric).value = metricsTracker.getMetricDataPoints(currentTime, metric)
            }
        }
    }

    fun getMetricData(metric: Metric): List<MetricValue> = metricData.getValue(metric).value

    /**
     * Returns the metric value at or just before [time] using binary search. Returns `null` if no data point exists at
     * or before the given time.
     */
    fun getMetricSample(metric: Metric, time: Instant): MetricValue? {
        val data = metricData.getValue(metric).value
        var index = data.binarySearch(MetricValue(time, Double.NaN), compareBy { it.time })
        if (index < 0) {
            // binarySearch returns -(insertionPoint + 1) on miss; convert to the index just before the insertion point
            index = -index - 2
        }
        if (index < 0) {
            return null
        }
        return data[index]
    }

    /** Get the most recent value for a metric, or null if no data yet. */
    fun getLatestValue(metric: Metric): Double? = metricData[metric]?.value?.lastOrNull()?.value
}
