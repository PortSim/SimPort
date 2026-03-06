package com.group7.metrics

import com.group7.Scenario

/**
 * Factory for creating metrics for individual nodes.
 *
 * Implementations create [MetricGroup] objects for specific node types. Returns null if no metric should be created for
 * a given node.
 *
 * @param NodeT the type of nodes this factory creates metrics for
 */
fun interface MetricFactory<in NodeT> {
    /**
     * Creates a metric group for the given node.
     *
     * @param node the [NodeT] to create a metric for
     * @param scenario the [Scenario] containing all nodes and metrics
     * @return a [MetricGroup] for this node, or null if no metric should be created
     */
    fun create(node: NodeT, scenario: Scenario): MetricGroup?
}
