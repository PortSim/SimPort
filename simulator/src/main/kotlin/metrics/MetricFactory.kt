package com.group7.metrics

fun interface MetricFactory<in NodeT> {
    fun createGroup(node: NodeT): MetricGroup
}
