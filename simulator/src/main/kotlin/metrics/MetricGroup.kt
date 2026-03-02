package com.group7.metrics

import com.group7.NodeGroup

sealed interface ResultColumn {
    val label: String
}

data class MetricColumn(override val label: String, val metric: Metric?, val format: String = "%.4f") : ResultColumn

data class LongColumn(override val label: String, val value: () -> Long?) : ResultColumn

/* Includes the raw metric and its moments. Can have no associatedNode for global metrics */
open class MetricGroup(val name: String, val associatedNode: NodeGroup?, val raw: Metric?, val moments: Moments?) {
    val allMetrics = listOfNotNull(raw, moments?.mean, moments?.lowerCi, moments?.upperCi, moments?.variance)

    /** Labels and metrics to show as columns in the results table. Override to customise. */
    open fun resultColumns(): List<ResultColumn> =
        listOf(
            MetricColumn("Mean", moments?.mean),
            MetricColumn("Lower CI", moments?.lowerCi),
            MetricColumn("Upper CI", moments?.upperCi),
            MetricColumn("Variance", moments?.variance),
            LongColumn("Samples") { moments?.sampleCount?.invoke() },
        )
}

class Moments(
    val mean: Metric,
    val lowerCi: Metric?,
    val upperCi: Metric?,
    val variance: Metric?,
    val sampleCount: (() -> Long)? = null,
)
