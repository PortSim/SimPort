import androidx.compose.runtime.mutableStateOf
import com.dk.kuiver.model.Kuiver
import com.dk.kuiver.model.KuiverEdge
import com.dk.kuiver.model.KuiverNode
import com.dk.kuiver.model.edge
import com.dk.kuiver.model.nodes
import com.group7.Node
import com.group7.OutputChannel
import com.group7.Scenario

internal class VisualisationModel(
    private val nodes: Map<Node, String>,
    private val edges: Map<Pair<String, String>, OutputChannel<*>>,
) {
    private val nodesById = nodes.entries.associate { (node, id) -> id to node }
    private val nodeMetrics = nodes.keys.associateWith { mutableStateOf(it.reportMetrics()) }
    private val channelStatuses = edges.values.associateWith { mutableStateOf(it.isOpen()) }

    val kuiver =
        Kuiver().also {
            it.nodes(nodes.values)
            for ((a, b) in edges.keys) {
                it.edge(a, b)
            }
        }

    fun refresh() {
        for ((node, metric) in nodeMetrics) {
            metric.value = node.reportMetrics()
        }
        for ((channel, status) in channelStatuses) {
            status.value = channel.isOpen()
        }
    }

    fun textForNode(node: KuiverNode) = nodesById.getValue(node.id).label

    fun metricsForNode(node: KuiverNode) = nodeMetrics.getValue(nodesById.getValue(node.id)).value

    fun isChannelOpen(edge: KuiverEdge) = channelStatuses.getValue(edges.getValue(edge.fromId to edge.toId)).value
}

internal fun VisualisationModel(scenario: Scenario): VisualisationModel {
    var nextId = 0

    val channels = mutableMapOf<Pair<String, String>, OutputChannel<*>>()
    val nodes = scenario.sources.associateWithTo(mutableMapOf<Node, _>()) { (nextId++).toString() }

    val queue = ArrayDeque<Node>(scenario.sources)
    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        for (channel in node.outgoing) {
            val downstream = channel.downstreamNode
            if (downstream !in nodes) {
                val id = (nextId++).toString()
                nodes[downstream] = id
                queue.addLast(downstream)
            }
            channels[nodes.getValue(node) to nodes.getValue(downstream)] = channel
        }
    }

    return VisualisationModel(nodes, channels)
}
