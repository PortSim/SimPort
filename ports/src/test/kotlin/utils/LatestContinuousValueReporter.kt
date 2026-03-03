package com.group7.utils

import com.group7.MetricReporter
import com.group7.Scenario
import com.group7.metrics.ContinuousMetric
import com.group7.metrics.Metric
import kotlin.time.Instant

class LatestContinuousValueReporter(private val scenario: Scenario) : MetricReporter {
    private val latestValues = mutableMapOf<ContinuousMetric, Double>()

    override fun report(currentTime: Instant) {
        for (metric in scenario.metrics.asSequence().flatMap { it.allMetrics }) {
            if (metric is ContinuousMetric) {
                latestValues[metric] = metric.report(currentTime)
            }
        }
    }

    fun getLatestValue(metric: Metric?) = latestValues[metric]
}
