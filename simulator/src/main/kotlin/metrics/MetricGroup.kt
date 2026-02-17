package com.group7.metrics

import com.group7.NodeGroup
import java.util.*

/* Includes the raw metric and its moments. Can have no associatedNode for global metrics */
open class MetricGroup(val name: String, val associatedNode: NodeGroup?, val raw: Metric, val moments: Moments?) {
    val allMetrics = listOfNotNull(raw, moments?.mean, moments?.lowerCi, moments?.upperCi, moments?.variance)

    /** Labels and metrics to show as columns in the results table. Override to customise. */
    open fun resultColumns(): List<Pair<String, Metric?>> =
        listOf(
            "Latest Value" to raw,
            "Mean" to moments?.mean,
            "Lower CI" to moments?.lowerCi,
            "Upper CI" to moments?.upperCi,
            "Variance" to moments?.variance,
        )

    override fun equals(other: Any?) =
        other is MetricGroup && other.name == name && other.associatedNode == associatedNode

    override fun hashCode() = Objects.hash(name, associatedNode)
}

class Moments(val mean: Metric, val lowerCi: Metric?, val upperCi: Metric?, val variance: Metric?)
