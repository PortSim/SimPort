package com.group7

import com.group7.metrics.MetricGroup

class Scenario(val sources: List<SourceNode>) {
    private val _metrics = mutableMapOf<Pair<String, NodeGroup?>, MetricGroup>()
    val metrics
        get() = _metrics.values

    val allNodeGroups by lazy(::walk)

    fun addMetric(metric: MetricGroup) {
        _metrics[metric.name to metric.associatedNode] = metric
    }

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
