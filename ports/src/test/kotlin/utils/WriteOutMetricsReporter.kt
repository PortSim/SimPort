package com.group7.utils

import com.group7.MetricReporter
import com.group7.Scenario
import com.group7.metrics.ContinuousMetric
import com.group7.metrics.InstantaneousMetric
import com.group7.metrics.MetricGroup
import com.group7.metrics.RateMetric
import kotlin.time.Instant

/** A metrics reporter implementation that writes out the metrics information into a string. */
class WriteOutMetricsReporter(val scenario: Scenario) : MetricReporter {
    private val metricResults = scenario.metrics.associateWith { MetricResults() }

    init {
        // Link to .onFire for instantaneous metrics
        for ((group, results) in metricResults) {
            for ((name, metric) in group.parts()) {
                when (metric) {
                    is InstantaneousMetric ->
                        metric.onFire { currentTime, value -> results.update(name, currentTime, value) }
                    is RateMetric -> metric.onFire { currentTime -> results.update(name, currentTime, null) }
                    is ContinuousMetric -> {}
                }
            }
        }
    }

    override fun report(currentTime: Instant) {
        // Update continuous metrics and write out
        for ((group, results) in metricResults) {
            for ((metricName, metric) in group.parts()) {
                if (metric is ContinuousMetric) {
                    val value = metric.report(currentTime)
                    results.update(metricName, currentTime, value)
                }
            }
        }
    }

    fun results() = metricResults.mapValues { (_, it) -> it.result() }
}

private class MetricResults {
    private var lastTimeSeen = Instant.DISTANT_PAST
    private val builder = StringBuilder()

    fun update(metricName: String?, currentTime: Instant, value: Double?) {
        if (value?.isNaN() == true) {
            return
        }
        val valueString = "%.5g".format(value)
        if (lastTimeSeen < currentTime) {
            builder.appendLine(currentTime)
            lastTimeSeen = currentTime
        }

        val line =
            when {
                value == null && metricName == null -> "Sample"
                value == null -> "$metricName Sample"
                metricName == null -> valueString
                else -> "$metricName: $valueString"
            }

        builder.append('\t')
        builder.appendLine(line)
    }

    fun result() = builder.toString()
}

private fun MetricGroup.parts() =
    sequenceOf(
            null to raw,
            "Mean" to moments?.mean,
            "Lower CI" to moments?.lowerCi,
            "Upper CI" to moments?.upperCi,
            "Variance" to moments?.variance,
        )
        .mapNotNull { (name, metric) -> metric?.let { name to it } }
        .toMap()
