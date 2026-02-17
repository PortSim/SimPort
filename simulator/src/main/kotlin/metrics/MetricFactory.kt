package com.group7.metrics

fun interface MetricFactory<in NodeT> {
    fun create(node: NodeT): MetricGroup?
}
