package com.group7.metrics

import com.group7.Scenario

fun interface MetricFactory<in NodeT> {
    fun create(node: NodeT, scenario: Scenario): MetricGroup?
}
