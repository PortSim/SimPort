import androidx.compose.runtime.mutableStateOf
import com.group7.Node
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
    val edgesToChannels: Map<Pair<Node, Node>, OutputChannel<*>>

    constructor(scenario: Scenario) {
        /* Enumerate all nodes and channels between them */
        val setOfNodes = mutableSetOf<Node>()
        val nodesOrdered = mutableListOf<Node>()
        val channels = mutableMapOf<Pair<Node, Node>, OutputChannel<*>>()
        val queue = ArrayDeque<Node>(scenario.sources)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (setOfNodes.add(node)) {
                nodesOrdered.add(node)
            }
            for (channel in node.outgoing) {
                val downstream = channel.downstreamNode
                if (downstream !in setOfNodes) {
                    queue.addLast(downstream)
                }
                channels[node to downstream] = channel
            }
        }

        sources = scenario.sources
        nodesOrderedByBFS = nodesOrdered
        edgesToChannels = channels
    }
}

class ScenarioLayout(scenario: Scenario) {
    private val graphOfScenario = ScenarioGraph(scenario)

    val elkGraphRoot: ElkNode = ElkGraphUtil.createGraph() // necessary so nodesContainer can have a input port

    init {
        elkGraphRoot.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered")
        elkGraphRoot.setProperty(LayeredOptions.CYCLE_BREAKING_STRATEGY, CycleBreakingStrategy.DEPTH_FIRST)
        // the cycle breaking property must be set in the nodes container because it only applies to its direct children
        // and not grandchildren
        elkGraphRoot.setProperty(LayeredOptions.CYCLE_BREAKING_STRATEGY, CycleBreakingStrategy.DEPTH_FIRST)
        elkGraphRoot.setProperty(LayeredOptions.NODE_PLACEMENT_STRATEGY, NodePlacementStrategy.BRANDES_KOEPF)
        elkGraphRoot.setProperty(LayeredOptions.NODE_PLACEMENT_BK_FIXED_ALIGNMENT, FixedAlignment.BALANCED)
        elkGraphRoot.setProperty(LayeredOptions.CONSIDER_MODEL_ORDER_STRATEGY, OrderingStrategy.PREFER_NODES)
    }

    private val simulationNodeToElkNode = buildMap {
        graphOfScenario.nodesOrderedByBFS.forEach { node ->
            val elkNode = ElkGraphUtil.createNode(elkGraphRoot)
            elkNode.width = 80.0
            elkNode.height = 80.0
            elkNode.identifier = node.label
            put(node, elkNode)
        }
    }

    private val simulationEdgeToElkEdge = buildMap {
        for ((sourceDest, channel) in graphOfScenario.edgesToChannels) {
            val edge =
                ElkGraphUtil.createSimpleEdge(
                    simulationNodeToElkNode[sourceDest.first],
                    simulationNodeToElkNode[sourceDest.second],
                )
            put(channel, edge)
        }
    }

    init {
        /* Code to generate the layout */
        LayoutMetaDataService.getInstance().registerLayoutMetaDataProviders(LayeredMetaDataProvider())
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
}
