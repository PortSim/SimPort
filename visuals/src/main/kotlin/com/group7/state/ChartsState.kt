package com.group7.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.group7.metrics.Metric
import com.group7.metrics.MetricValue
import com.group7.metrics.MetricsTracker
import kotlin.time.Instant

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
                metricData.getValue(metric).value = metricsTracker.getMetricDataPoints(metric)
            }
        }
    }

    fun getMetricData(metric: Metric): List<MetricValue> = downsample(metricData.getValue(metric).value)

    fun getMetricSample(metric: Metric, time: Instant): MetricValue? {
        val data = metricData.getValue(metric).value
        var index = data.binarySearch(MetricValue(time, Double.NaN), compareBy { it.time })
        if (index < 0) {
            index = (-(index + 1) - 1)
        }
        if (index < 0) {
            return null
        }
        return data[index]
    }

    /** Get the most recent value for a metric, or null if no data yet. */
    fun getLatestValue(metric: Metric): Double? = metricData[metric]?.value?.lastOrNull()?.value
}

private const val DESIRED_SAMPLES = 2000

private fun downsample(samples: List<MetricValue>): MutableList<MetricValue> {
    val result = samples.toMutableList()
    while (result.size >= 2 * DESIRED_SAMPLES) {
        val newCount = result.size / 2
        for (i in 0..<newCount) {
            val b1 = result[2 * i]
            val b2 = result[2 * i + 1]
            result[i] =
                MetricValue(
                    Instant.fromEpochMilliseconds((b1.time.toEpochMilliseconds() + b2.time.toEpochMilliseconds()) / 2),
                    (b1.value + b2.value) / 2,
                )
        }

        result.subList(newCount, result.size).clear()
    }
    return result
}
