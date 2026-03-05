package com.group7.metrics

import com.group7.MetricReporter
import com.group7.Scenario
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf

data class MetricValue(val time: Instant, val value: Double)

class MetricsTracker(scenario: Scenario) : MetricReporter {
    val metricGroups = scenario.metrics.groupBy { it.name }
    val allMetrics = metricGroups.values.asSequence().flatten().flatMap { it.allMetrics }.toList()
    private val continuousMetrics = allMetrics.filterIsInstance<ContinuousMetric>()
    private val buffer = allMetrics.associateWithTo(mutableMapOf()) { persistentListOf<MetricValue>() }

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
