package com.group7.metrics

import com.group7.Scenario

fun interface GlobalMetricFactory {
    fun create(scenario: Scenario): MetricGroup
}
