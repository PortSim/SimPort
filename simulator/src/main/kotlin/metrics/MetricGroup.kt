package com.group7.metrics

import com.group7.NodeGroup

class MetricGroup(val name: String, val associatedNode: NodeGroup?, val raw: Metric, val moments: Moments?) {
    val allMetrics = listOfNotNull(raw, moments?.mean, moments?.lowerCi, moments?.upperCi, moments?.variance)
}

class Moments(val mean: Metric, val lowerCi: Metric?, val upperCi: Metric?, val variance: Metric?)
