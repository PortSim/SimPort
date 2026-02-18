package com.group7.metrics

import com.group7.DisplayProperty
import com.group7.DoubleDisplayProperty
import com.group7.GroupDisplayProperty
import com.group7.MetricGroupDisplayProperty
import com.group7.NodeGroup
import com.group7.TextDisplayProperty
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

    fun displayProperty(): DisplayProperty =
        GroupDisplayProperty(
            name,
            buildList {
                add(MetricGroupDisplayProperty(this@MetricGroup))
                if (raw.lastData != null) {
                    add(DoubleDisplayProperty("Raw", raw.lastData!!.second, ""))
                } else {
                    add(TextDisplayProperty("Raw: no data yet"))
                }
                if (moments != null) {
                    // TODO more informative messages on why values are not available
                    moments.mean.lastData?.let { add(DoubleDisplayProperty("Mean", it.second, "")) }
                    moments.lowerCi?.lastData?.let { add(DoubleDisplayProperty("Lower CI", it.second, "")) }
                    moments.upperCi?.lastData?.let { add(DoubleDisplayProperty("Upper CI", it.second, "")) }
                    moments.variance?.lastData?.let { add(DoubleDisplayProperty("Variance", it.second, "")) }
                }
            },
        )
}

class Moments(val mean: Metric, val lowerCi: Metric?, val upperCi: Metric?, val variance: Metric?)
