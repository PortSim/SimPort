package com.group7.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dynatrace.dynahist.Histogram
import com.dynatrace.dynahist.layout.LogQuadraticLayout
import com.group7.metrics.InstantaneousMetric
import com.group7.metrics.Metric
import com.group7.metrics.MetricsTracker
import kotlin.time.Instant

class HistogramsState(metricsTracker: MetricsTracker) {
    var latestTimeSeen by mutableStateOf(Instant.DISTANT_PAST)
        private set

    /** Per-metric histograms for instantaneous metrics, recorded incrementally via onFire. */
    private val histograms =
        metricsTracker.allMetrics.filterIsInstance<InstantaneousMetric>().associateWith {
            Histogram.createDynamic(LogQuadraticLayout.create(1e-5, 1e-2, -1e15, 1e15))
        }

    init {
        metricsTracker.allMetrics.filterIsInstance<InstantaneousMetric>().forEach { metric ->
            metric.onFire { currentTime, value ->
                histograms.getValue(metric).addValue(value)
                latestTimeSeen = currentTime
            }
        }
    }

    fun getHistogram(metric: Metric) = histograms[metric]
}
