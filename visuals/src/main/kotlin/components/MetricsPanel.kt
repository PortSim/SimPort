package components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.dynatrace.dynahist.Histogram
import com.dynatrace.dynahist.layout.LogQuadraticLayout
import com.group7.MetricReporter
import com.group7.Scenario
import com.group7.metrics.ContinuousMetric
import com.group7.metrics.InstantaneousMetric
import com.group7.metrics.Metric
import kotlin.math.abs
import kotlin.time.Instant

/** Samples metrics from nodes at regular intervals, storing data as Compose-observable mutable state. */
class MetricsPanelState(val scenario: Scenario, private val redrawEveryNSamples: Int = 10) : MetricReporter {
    val metricGroups = scenario.metrics.groupBy { it.name }

    private val allMetrics = metricGroups.values.asSequence().flatten().flatMap { it.allMetrics }.toList()

    /** Per-metric time series data using Compose-observable SnapshotStateLists */
    private val metricData: Map<Metric, SnapshotStateList<Pair<Instant, Double>>> =
        allMetrics.associateWith { mutableStateListOf() }

    /** Buffer for collecting samples before flushing to UI */
    private val buffer: Map<Metric, MutableList<Pair<Instant, Double>>> = allMetrics.associateWith { mutableListOf() }
    private var isBatching = false
    private var samplesSinceRedraw = 0
    var latestTimeSeen by mutableStateOf(Instant.DISTANT_PAST)
        private set

    private var samplesSeen = 0

    /** Per-metric histograms for instantaneous metrics, recorded incrementally via onFire. */
    private val histograms: Map<Metric, Histogram> =
        allMetrics.filterIsInstance<InstantaneousMetric>().associateWith {
            Histogram.createDynamic(LogQuadraticLayout.create(1e-5, 1e-2, -1e15, 1e15))
        }

    init {
        for (metric in allMetrics) {
            if (metric is InstantaneousMetric) {
                metric.onFire { currentTime, value ->
                    if (!value.isNaN()) {
                        buffer.getValue(metric).add(currentTime to value)
                        histograms.getValue(metric).addValue(value)
                    }
                }
            }
        }
    }

    fun getHistogram(metric: Metric): Histogram? = histograms[metric]

    /** Get time series data for a specific node */
    fun getMetricData(metric: Metric): List<Pair<Instant, Double>> = downsample(metricData.getValue(metric))

    fun getMetricSample(metric: Metric, time: Instant): Pair<Instant, Double>? {
        val data = metricData.getValue(metric)
        var index = data.binarySearch(time to Double.NaN, compareBy { it.first })
        if (index < 0) {
            index = (-(index + 1) - 1)
        }
        if (index < 0) {
            return null
        }
        return data[index]
    }

    /** Get the most recent value for a metric, or null if no data yet. */
    fun getLatestValue(metric: Metric): Double? = getMetricSample(metric, latestTimeSeen)?.second

    /** Start batch mode - samples will be buffered without triggering UI updates */
    fun beginBatch() {
        isBatching = true
    }

    /** End batch mode - flush all buffered samples to UI in one update */
    fun endBatch() {
        if (isBatching) {
            flushBuffer()
            isBatching = false
        }
    }

    private fun flushBuffer() {
        Snapshot.withMutableSnapshot {
            for (metric in allMetrics) {
                val buffered = buffer[metric]
                if (buffered.isNullOrEmpty()) {
                    continue
                }
                if (metric is ContinuousMetric) {
                    if (buffered.last().first < latestTimeSeen) {
                        // End final sample if needed
                        buffered.add(latestTimeSeen to buffered.last().second)
                    }
                }
                metricData.getValue(metric).addAll(buffered)
            }
        }
        buffer.values.forEach { it.clear() }
        samplesSinceRedraw = 0
    }

    override fun report(currentTime: Instant) {
        samplesSeen++
        updateMetrics(currentTime)

        if (!isBatching) {
            samplesSinceRedraw++
            if (samplesSinceRedraw >= redrawEveryNSamples) {
                flushBuffer()
            }
        }
    }

    private fun updateMetrics(currentTime: Instant) {
        for (metric in allMetrics) {
            if (metric is ContinuousMetric) {
                val value = metric.report(currentTime)
                if (value.isNaN()) {
                    continue
                }
                val data = buffer.getValue(metric)
                if (data.isEmpty()) {
                    data.add(currentTime to value)
                    continue
                }
                if (abs(data.last().second - value) < 1e-2) {
                    continue
                }
                if (data.last().first < latestTimeSeen) {
                    // End old value
                    data.add(latestTimeSeen to data.last().second)
                }
                data.add(currentTime to value)
            }
        }

        latestTimeSeen = currentTime
    }
}

private const val DESIRED_SAMPLES = 2000

private fun downsample(samples: List<Pair<Instant, Double>>): MutableList<Pair<Instant, Double>> {
    val result = samples.toMutableList()
    while (result.size >= 2 * DESIRED_SAMPLES) {
        val newCount = result.size / 2
        for (i in 0..<newCount) {
            val b1 = result[2 * i]
            val b2 = result[2 * i + 1]
            result[i] =
                Pair(
                    Instant.fromEpochMilliseconds(
                        (b1.first.toEpochMilliseconds() + b2.first.toEpochMilliseconds()) / 2
                    ),
                    (b1.second + b2.second) / 2,
                )
        }

        result.subList(newCount, result.size).clear()
    }
    return result
}
