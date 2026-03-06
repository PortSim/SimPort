package com.group7.metrics

import kotlin.time.Instant
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

internal sealed interface MetricData {
    val values: PersistentList<MetricValue>

    fun add(currentTime: Instant, value: Double)
}

internal fun MetricData(isContinuous: Boolean, downsample: Boolean) =
    when {
        !downsample -> RawMetricData()
        isContinuous -> DownsampledContinuousMetricData()
        else -> DownsampledInstantaneousMetricData()
    }

private class RawMetricData : MetricData {
    override var values = persistentListOf<MetricValue>()
        private set

    override fun add(currentTime: Instant, value: Double) {
        values = values.add(MetricValue(currentTime, value))
    }
}
