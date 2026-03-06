package com.group7.metrics

import kotlin.time.Instant
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/**
 * Storage interface for metric data points.
 *
 * Implementations handle storing and optionally downsampling metric values collected during simulation.
 * Different implementations may apply optimizations like downsampling to manage memory usage while
 * preserving important features in the data.
 */
internal sealed interface MetricData {
    /** All metric values stored in this data object, in chronological order. */
    val values: PersistentList<MetricValue>

    /**
     * Adds a new metric value to this data store.
     *
     * @param currentTime the simulation time at which the value was recorded
     * @param value the numeric value of the metric
     */
    fun add(currentTime: Instant, value: Double)
}

/**
 * Factory function to create appropriate [MetricData] implementation.
 *
 * Selects between raw storage and downsampling strategies based on metric type and configuration.
 *
 * @param isContinuous whether the metric is continuous (reports at each time step) or instantaneous (reports on discrete events)
 * @param downsample whether to apply downsampling for performance optimization
 * @return a [MetricData] implementation suitable for the given configuration
 */
internal fun MetricData(isContinuous: Boolean, downsample: Boolean) =
    when {
        !downsample -> RawMetricData()
        isContinuous -> DownsampledContinuousMetricData()
        else -> DownsampledInstantaneousMetricData()
    }

/**
 * Stores all metric data without any downsampling.
 *
 * Preserves the complete history of all metric values recorded during simulation.
 */
private class RawMetricData : MetricData {
    override var values = persistentListOf<MetricValue>()
        private set

    override fun add(currentTime: Instant, value: Double) {
        values = values.add(MetricValue(currentTime, value))
    }
}
