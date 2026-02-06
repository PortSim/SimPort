package utils

import ScenarioGraph
import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.properties.Container

fun Scenario.metricsNodes(): List<NodeGroup> {
    val result = ScenarioGraph(this).nodesOrderedByBFS.toMutableList<NodeGroup>()
    val seenParents = mutableSetOf<NodeGroup>()
    for (node in result) {
        var parent = node.parent
        while (parent != null && seenParents.add(parent)) {
            parent = parent.parent
        }
    }
    result.addAll(seenParents)
    result.retainAll { it is Container }
    result.sortBy { it.label }
    return result
}
