package com.group7.metrics

import com.group7.NodeGroup

/**
 * Base interface for columns in metric result tables.
 *
 * Different column types display different kinds of information about metrics.
 */
sealed interface ResultColumn {
    /** The label to display for this column. */
    val label: String
}

/**
 * A result column that displays a [Metric] value with a specified format.
 *
 * @property label the column header label
 * @property metric the metric to display (or null)
 * @property format the printf-style format string for the value
 */
data class MetricColumn(override val label: String, val metric: Metric?, val format: String = "%.4f") : ResultColumn

/**
 * A result column that displays a long integer value.
 *
 * @property label the column header label
 * @property value a lambda that provides the value for each row
 */
data class LongColumn(override val label: String, val value: () -> Long?) : ResultColumn

/**
 * Groups related metrics (raw metric and its statistical moments) together.
 *
 * A metric group can be associated with a specific [NodeGroup] or represent global metrics. It includes the raw metric
 * and computed moments (mean, confidence intervals, variance).
 *
 * @property name the name of this metric group
 * @property associatedNode the [NodeGroup] this metric is associated with (null for global metrics)
 * @property raw the raw metric (null if only moments are tracked)
 * @property moments the computed statistical moments (null if only raw metric is tracked)
 */
open class MetricGroup(val name: String, val associatedNode: NodeGroup?, val raw: Metric?, val moments: Moments?) {
    /** All individual metrics contained in this group (raw + moments). */
    val allMetrics = listOfNotNull(raw, moments?.mean, moments?.lowerCi, moments?.upperCi, moments?.variance)

    /**
     * Returns the columns to display for this metric group in a results table.
     *
     * Override to customize which columns and in what order are shown.
     *
     * @return a list of [ResultColumn] objects describing what to display
     */
    open fun resultColumns(): List<ResultColumn> =
        listOf(
            MetricColumn("Mean", moments?.mean),
            MetricColumn("Lower CI", moments?.lowerCi),
            MetricColumn("Upper CI", moments?.upperCi),
            MetricColumn("Variance", moments?.variance),
            LongColumn("Samples") { moments?.sampleCount?.invoke() },
        )
}

/**
 * Statistical moments of a metric (mean, confidence intervals, variance).
 *
 * @property mean the mean (expected value) of the metric
 * @property lowerCi the lower bound of the confidence interval (null if not computed)
 * @property upperCi the upper bound of the confidence interval (null if not computed)
 * @property variance the variance of the metric (null if not computed)
 * @property sampleCount a lambda that returns the number of samples (null if not available)
 */
class Moments(
    val mean: Metric,
    val lowerCi: Metric?,
    val upperCi: Metric?,
    val variance: Metric?,
    val sampleCount: (() -> Long)? = null,
)
