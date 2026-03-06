package com.group7

import com.group7.metrics.MetricGroup

/**
 * Represents the structure and configuration of a simulation.
 *
 * A scenario defines the source nodes that generate entities, the network of intermediate nodes and channels they form,
 * and the metrics to be collected during simulation.
 *
 * @property sources the source nodes that generate entities into the network
 */
class Scenario(val sources: List<SourceNode>) {
    private val _metrics = mutableMapOf<Pair<String, NodeGroup?>, MetricGroup>()

    /** All metrics registered with this scenario. */
    val metrics
        get() = _metrics.values

    /** All node groups (both individual nodes and grouped nodes) in the scenario. */
    val allNodeGroups by lazy(::walk)

    inline fun <reified T> every() = allNodeGroups.asSequence().filterIsInstance<T>()

    /**
     * Registers a metric with this scenario.
     *
     * @param metric the [MetricGroup] to add
     */
    fun addMetric(metric: MetricGroup) {
        _metrics[metric.name to metric.associatedNode] = metric
    }

    /**
     * Performs a breadth-first search to find all nodes reachable from source nodes.
     *
     * @return a list of all nodes in the network
     */
    fun bfs(): List<Node> {
        val queue = ArrayDeque<Node>(sources)
        val seenNodes = queue.toMutableSet()

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            for (outgoing in node.outgoing) {
                val downstream = outgoing.downstream.downstreamNode
                if (seenNodes.add(downstream)) {
                    queue.addLast(downstream)
                }
            }
        }

        // This will be ordered in the same order as the set, since the set will be a LinkedHashSet
        return seenNodes.toList()
    }

    /**
     * Recursively traverses the node hierarchy to find all node groups.
     *
     * Performs a BFS to find all nodes, then walks up the parent hierarchy to include all parent node groups.
     *
     * @return a set of all node groups (nodes and their parents) in the scenario
     */
    private fun walk(): Set<NodeGroup> {
        val nodes = bfs().toMutableSet<NodeGroup>()

        val seenParents = mutableSetOf<NodeGroup>()
        for (node in nodes) {
            var parent = node.parent
            while (parent != null && seenParents.add(parent)) {
                parent = parent.parent
            }
        }
        nodes.addAll(seenParents)
        return nodes
    }
}
