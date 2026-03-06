package com.group7.metrics

import kotlin.time.Instant
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/** Stores metric values collected throughout the simulation */
internal sealed interface MetricData {
    val values: PersistentList<MetricValue>

    /** Adds time tagged data into internal logs */
    fun add(currentTime: Instant, value: Double)
}

/** Constructs the appropriate MetricData implementation given the type of data required */
internal fun MetricData(isContinuous: Boolean, downsample: Boolean) =
    when {
        !downsample -> RawMetricData()
        isContinuous -> DownsampledContinuousMetricData()
        else -> DownsampledInstantaneousMetricData()
    }

/** Stores raw time-value data */
private class RawMetricData : MetricData {
    override var values = persistentListOf<MetricValue>()
        private set

    override fun add(currentTime: Instant, value: Double) {
        values = values.add(MetricValue(currentTime, value))
    }
}
