package com.group7.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.dynatrace.dynahist.Histogram
import com.dynatrace.dynahist.layout.LogQuadraticLayout
import com.group7.metrics.InstantaneousMetric
import com.group7.metrics.Metric
import com.group7.metrics.MetricsTracker
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Instant

class HistogramsState(metricsTracker: MetricsTracker) {
    var latestTimeSeen by mutableStateOf(Instant.DISTANT_PAST)
        private set

    /** Per-metric histograms for instantaneous metrics, recorded incrementally via onFire. */
    private val histograms =
        metricsTracker.allMetrics.filterIsInstance<InstantaneousMetric>().associateWith {
            Histogram.createDynamic(LogQuadraticLayout.create(1e-5, 1e-2, -1e15, 1e15))
        }

    private val updateActions = ConcurrentLinkedQueue<() -> Unit>()
    private var numUpdates = AtomicInteger(0)

    init {
        metricsTracker.allMetrics.filterIsInstance<InstantaneousMetric>().forEach { metric ->
            metric.onFire { _, value ->
                val histogram = histograms.getValue(metric)
                // Histograms are not thread-safe, so we enqueue updates which will later be performed on the UI
                // thread
                updateActions.add { histogram.addValue(value) }
                numUpdates.incrementAndGet()
            }
        }
    }

    // Must be called on the UI thread
    fun getHistogram(metric: Metric) = histograms[metric]

    suspend fun updateLive() {
        while (true) {
            // Wait for next frame
            withFrameNanos { flushUpdates() }
        }
    }

    fun update(currentTime: Instant) {
        flushUpdates()
        latestTimeSeen = currentTime
    }

    private fun flushUpdates() {
        val numUpdates = numUpdates.getAndSet(0)
        repeat(numUpdates) {
            val action = updateActions.remove()
            action()
        }
    }
}
