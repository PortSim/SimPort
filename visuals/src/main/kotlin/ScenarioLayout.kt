import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.google.common.collect.HashBiMap
import com.group7.FieldDisplayProperty
import com.group7.GroupDisplayProperty
import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.channels.*
import org.eclipse.elk.alg.layered.options.*
import org.eclipse.elk.core.RecursiveGraphLayoutEngine
import org.eclipse.elk.core.data.LayoutMetaDataService
import org.eclipse.elk.core.options.CoreOptions
import org.eclipse.elk.core.options.EdgeLabelPlacement
import org.eclipse.elk.core.options.HierarchyHandling
import org.eclipse.elk.core.util.BasicProgressMonitor
import org.eclipse.elk.graph.ElkNode
import org.eclipse.elk.graph.util.ElkGraphUtil

class ScenarioLayout(scenario: Scenario, iconProvider: IconProvider?) {
    private val nodesOrderedByBFS = scenario.bfs()
    private val edgesWithChannels =
        nodesOrderedByBFS.flatMap { upstream ->
            upstream.outgoing.asSequence().map { channel ->
                Triple(upstream, channel.downstream.downstreamNode, channel)
            }
        }

    val elkGraphRoot: ElkNode = ElkGraphUtil.createGraph() // necessary so nodesContainer can have a input port

    fun setElkContainerNodeProperties(elkNode: ElkNode) {
        // the cycle breaking property must be set in the nodes container because it only applies to its direct children
        // and not grandchildren
        elkNode.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered")
        elkNode.setProperty(LayeredOptions.NODE_PLACEMENT_STRATEGY, NodePlacementStrategy.BRANDES_KOEPF)
        elkNode.setProperty(LayeredOptions.NODE_PLACEMENT_BK_FIXED_ALIGNMENT, FixedAlignment.BALANCED)
        elkNode.setProperty(LayeredOptions.CONSIDER_MODEL_ORDER_STRATEGY, OrderingStrategy.PREFER_EDGES)
        elkNode.setProperty(LayeredOptions.CYCLE_BREAKING_STRATEGY, CycleBreakingStrategy.DFS_NODE_ORDER)
        elkNode.setProperty(LayeredOptions.SPACING_EDGE_NODE_BETWEEN_LAYERS, 15.0)
        elkNode.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.INCLUDE_CHILDREN)
    }

    init {
        setElkContainerNodeProperties(elkGraphRoot)
    }

    private val nodeGroups = mutableMapOf<NodeGroup, ElkNode>()

    fun getElkNodeFromNodeGroup(nodeGroup: NodeGroup?): ElkNode {
        if (nodeGroup == null) {
            return elkGraphRoot
        }
        if (!nodeGroups.containsKey(nodeGroup)) {
            val elkNode = ElkGraphUtil.createNode(getElkNodeFromNodeGroup(nodeGroup.parent))
            elkNode.identifier = nodeGroup.label
            setElkContainerNodeProperties(elkNode)
            nodeGroups[nodeGroup] = elkNode
        }
        return nodeGroups[nodeGroup]!!
    }

    private val simulationNodeGroupToElkNode =
        HashBiMap.create(
            buildMap {
                nodesOrderedByBFS.forEach { node ->
                    val elkNode = ElkGraphUtil.createNode(getElkNodeFromNodeGroup(node.parent))
                    elkNode.width = 80.0
                    elkNode.height = 80.0
                    elkNode.identifier = node.label
                    put(node, elkNode)
                }
                putAll(nodeGroups)
            }
        )

    fun getNodeGroupFromElkNode(elkNode: ElkNode) = simulationNodeGroupToElkNode.inverse()[elkNode]

    private val simulationEdgeToElkEdge = buildMap {
        for ((source, destination, channel) in edgesWithChannels) {
            val edge =
                ElkGraphUtil.createSimpleEdge(
                    simulationNodeGroupToElkNode[source],
                    simulationNodeGroupToElkNode[destination],
                )
            ElkGraphUtil.updateContainment(edge)
            val label =
                ElkGraphUtil.createLabel(
                    "1", // placeholder text of 1 so that ELK layouts the labels appropriately
                    edge,
                )
            edge.labels.add(label)
            label.setProperty(LayeredOptions.EDGE_LABELS_PLACEMENT, EdgeLabelPlacement.CENTER)
            label.setProperty(CoreOptions.EDGE_LABELS_INLINE, false)
            label.width = 60.0
            label.height = 20.0
            put(channel, edge)
        }
    }

    init {
        /* Code to generate the layout */
        RecursiveGraphLayoutEngine().layout(elkGraphRoot, BasicProgressMonitor())
    }

    private fun getDisplayPropertyForNodeGroup(nodeGroup: NodeGroup): GroupDisplayProperty {
        val properties =
            listOfNotNull(nodeGroup::class.simpleName?.let { FieldDisplayProperty("Class name", it) }) +
                nodeGroup.properties()
        return GroupDisplayProperty(nodeGroup.label, properties)
    }

    private fun getDisplayPropertyForGlobalNode(): GroupDisplayProperty {
        return GroupDisplayProperty("Global Port")
    }

    val nodeMetrics =
        simulationNodeGroupToElkNode.entries.associate { (node, elkNode) ->
            elkNode to mutableStateOf(node.reportMetrics())
        }
    val edgeStatuses =
        simulationEdgeToElkEdge.entries.associate { (channel, edge) -> edge to mutableStateOf(channel.openStatus()) }
    val edgeCounts =
        simulationEdgeToElkEdge.entries.associate { (channel, edge) ->
            edge to mutableStateOf(channel.transmissionCount)
        }
    val nodeDisplayProperties: Map<ElkNode, MutableState<GroupDisplayProperty>> = buildMap {
        simulationNodeGroupToElkNode.forEach { (nodeGroup, elkNode) ->
            put(elkNode, mutableStateOf(getDisplayPropertyForNodeGroup(nodeGroup)))
        }
        put(elkGraphRoot, mutableStateOf(getDisplayPropertyForGlobalNode()))
    }

    val iconConfig: Map<ElkNode, NodeIcon> = buildMap {
        nodesOrderedByBFS.forEach { node ->
            // 1. Generate the icon for the simulation node
            val icon = iconProvider?.generateIcon(node)

            // 2. Find the ElkNode that corresponds to THIS simulation node
            val elkNode = simulationNodeGroupToElkNode[node]

            if (icon != null && elkNode != null) {
                put(elkNode, icon)
            }
        }
    }

    fun refresh() {
        simulationNodeGroupToElkNode.forEach { (node, elkNode) ->
            nodeMetrics.getValue(elkNode).value = node.reportMetrics()
        }
        simulationEdgeToElkEdge.forEach { (channel, elkEdge) ->
            edgeStatuses.getValue(elkEdge).value = channel.openStatus()
        }
        simulationEdgeToElkEdge.forEach { (channel, elkEdge) ->
            edgeCounts.getValue(elkEdge).value = channel.transmissionCount
        }
        simulationNodeGroupToElkNode.forEach { (nodeGroup, elkNode) ->
            nodeDisplayProperties.getValue(elkNode).value = getDisplayPropertyForNodeGroup(nodeGroup)
        }
        nodeDisplayProperties.getValue(elkGraphRoot).value = getDisplayPropertyForGlobalNode()
    }

    private companion object {
        init {
            LayoutMetaDataService.getInstance().registerLayoutMetaDataProviders(LayeredMetaDataProvider())
        }
    }
}

class EdgeStatus(val openStatus: Boolean = false, val channelType: ChannelType<*>)

private fun OutputChannel<*, *>.openStatus(): EdgeStatus =
    EdgeStatus(
        if (this.isPush()) {
            this.isOpen()
        } else {
            this.downstream.isReady()
        },
        if (this.isPush()) {
            ChannelType.Push
        } else {
            ChannelType.Pull
        },
    )
