import androidx.compose.runtime.mutableStateOf
import com.group7.Node
import com.group7.NodeGroup
import com.group7.OutputChannel
import com.group7.Scenario
import org.eclipse.elk.alg.layered.options.CycleBreakingStrategy
import org.eclipse.elk.alg.layered.options.FixedAlignment
import org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider
import org.eclipse.elk.alg.layered.options.LayeredOptions
import org.eclipse.elk.alg.layered.options.NodePlacementStrategy
import org.eclipse.elk.alg.layered.options.OrderingStrategy
import org.eclipse.elk.core.RecursiveGraphLayoutEngine
import org.eclipse.elk.core.data.LayoutMetaDataService
import org.eclipse.elk.core.options.CoreOptions
import org.eclipse.elk.core.options.HierarchyHandling
import org.eclipse.elk.core.util.BasicProgressMonitor
import org.eclipse.elk.graph.ElkNode
import org.eclipse.elk.graph.util.ElkGraphUtil

/*
   The scenario graph figures out the nodes and channels in a scenario.
   It might be simpler to have a function in scenario instead, i am undecided.
*/
internal class ScenarioGraph {
    val sources: List<Node>
    val nodesOrderedByBFS: List<Node>
    val edgesWithChannels: List<Triple<Node, Node, OutputChannel<*>>>

    constructor(scenario: Scenario) {
        /* Enumerate all nodes and channels between them */
        val setOfNodes = mutableSetOf<Node>()
        val nodesOrdered = mutableListOf<Node>()
        val channels = mutableListOf<Triple<Node, Node, OutputChannel<*>>>()
        val queue = ArrayDeque<Node>(scenario.sources)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (setOfNodes.add(node)) {
                nodesOrdered.add(node)
                for (channel in node.outgoing) {
                    val downstream = channel.downstreamNode
                    if (downstream !in setOfNodes) {
                        queue.addLast(downstream)
                    }
                    channels.add(Triple(node, downstream, channel))
                }
            }
        }

        sources = scenario.sources
        nodesOrderedByBFS = nodesOrdered
        edgesWithChannels = channels
    }
}

class ScenarioLayout(scenario: Scenario) {
    private val graphOfScenario = ScenarioGraph(scenario)

    val elkGraphRoot: ElkNode = ElkGraphUtil.createGraph() // necessary so nodesContainer can have a input port

    fun setElkContainerNodeProperties(elkNode: ElkNode) {
        // the cycle breaking property must be set in the nodes container because it only applies to its direct children
        // and not grandchildren
        elkNode.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered")
        elkNode.setProperty(LayeredOptions.NODE_PLACEMENT_STRATEGY, NodePlacementStrategy.BRANDES_KOEPF)
        elkNode.setProperty(LayeredOptions.NODE_PLACEMENT_BK_FIXED_ALIGNMENT, FixedAlignment.BALANCED)
        elkNode.setProperty(LayeredOptions.CONSIDER_MODEL_ORDER_STRATEGY, OrderingStrategy.PREFER_EDGES)
        elkNode.setProperty(LayeredOptions.CYCLE_BREAKING_STRATEGY, CycleBreakingStrategy.DFS_NODE_ORDER)
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

    private val simulationNodeToElkNode = buildMap {
        graphOfScenario.nodesOrderedByBFS.forEach { node ->
            val elkNode = ElkGraphUtil.createNode(getElkNodeFromNodeGroup(node.parent))
            elkNode.width = 80.0
            elkNode.height = 80.0
            elkNode.identifier = node.label
            put(node, elkNode)
        }
    }

    private val simulationEdgeToElkEdge = buildMap {
        for ((source, dest, channel) in graphOfScenario.edgesWithChannels) {
            val edge = ElkGraphUtil.createSimpleEdge(simulationNodeToElkNode[source], simulationNodeToElkNode[dest])
            ElkGraphUtil.updateContainment(edge)
            put(channel, edge)
        }
    }

    init {
        /* Code to generate the layout */
        RecursiveGraphLayoutEngine().layout(elkGraphRoot, BasicProgressMonitor())
    }

    val nodeMetrics =
        simulationNodeToElkNode.entries.associate { (node, elkNode) -> elkNode to mutableStateOf(node.reportMetrics()) }
    val edgeStatuses =
        simulationEdgeToElkEdge.entries.associate { (channel, edge) -> edge to mutableStateOf(channel.isOpen()) }

    fun refresh() {
        simulationNodeToElkNode.forEach { (node, elkNode) ->
            nodeMetrics.getValue(elkNode).value = node.reportMetrics()
        }
        simulationEdgeToElkEdge.forEach { (channel, elkEdge) ->
            edgeStatuses.getValue(elkEdge).value = channel.isOpen()
        }
    }

    private companion object {
        init {
            LayoutMetaDataService.getInstance().registerLayoutMetaDataProviders(LayeredMetaDataProvider())
        }
    }
}
