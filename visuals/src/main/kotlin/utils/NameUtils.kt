package utils

import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.properties.Container

fun assignNodeNames(scenario: Scenario): Map<NodeGroup, String> {
    val usedNames = mutableSetOf<String>()
    val assignedNodeNames = mutableMapOf<NodeGroup, String>()

    for (node in scenario.allNodes) {
        if (node !is Container<*>) {
            continue
        }
        var label = node.label
        var i = 2
        while (label in usedNames) {
            label = "${node.label} (${i++})"
        }
        usedNames.add(label)
        assignedNodeNames[node] = label
    }

    return assignedNodeNames
}
