package com.group7.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.group7.*
import com.group7.channels.*
import com.group7.properties.FieldDisplayProperty
import com.group7.properties.GroupDisplayProperty

class PortDisplayState(scenario: Scenario, iconProvider: IconProvider = IconProvider.defaultProvider()) {
    private val nodesOrderedByBFS = scenario.bfs()
    private val allChannels = nodesOrderedByBFS.flatMap { it.outgoing }

    private val nodeStates =
        scenario.allNodeGroups.associateWith { node ->
            NodeDisplayState(node, (node as? Node)?.let(iconProvider::generateIcon))
        }

    private val edgeStates = allChannels.associateWith { EdgeDisplayState(it) }

    fun getNodeGroupState(node: NodeGroup) = nodeStates.getValue(node)

    fun getEdgeState(channel: OutputChannel<*, *>) = edgeStates.getValue(channel)

    fun refresh() {
        nodeStates.values.forEach(NodeDisplayState::refresh)
        edgeStates.values.forEach(EdgeDisplayState::refresh)
    }
}

class NodeDisplayState(private val nodeGroup: NodeGroup, val icon: NodeIcon?) {
    var occupancy by mutableStateOf(nodeGroup.reportOccupants())
        private set

    var displayProperties by mutableStateOf(getDisplayProperty())

    private fun getDisplayProperty(): GroupDisplayProperty {
        val properties =
            listOfNotNull(nodeGroup::class.simpleName?.let { FieldDisplayProperty("Class name", it) }) +
                nodeGroup.properties()
        return GroupDisplayProperty(nodeGroup.label, properties)
    }

    fun refresh() {
        occupancy = nodeGroup.reportOccupants()
        displayProperties = getDisplayProperty()
    }
}

class EdgeDisplayState(private val channel: OutputChannel<*, *>) {
    val channelType = if (channel.isPush()) ChannelType.Push else ChannelType.Pull
    var openStatus by mutableStateOf(false)
        private set

    var transmissionCount by mutableStateOf(0)
        private set

    init {
        refresh()
    }

    fun refresh() {
        openStatus =
            if (channel.isPush()) {
                channel.isOpen()
            } else {
                channel.downstream.isReady()
            }
        transmissionCount = channel.transmissionCount
    }
}
