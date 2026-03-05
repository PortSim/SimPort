package com.group7

import com.group7.channels.OutputChannel
import org.eclipse.elk.alg.layered.options.*
import org.eclipse.elk.core.RecursiveGraphLayoutEngine
import org.eclipse.elk.core.data.LayoutMetaDataService
import org.eclipse.elk.core.options.CoreOptions
import org.eclipse.elk.core.options.EdgeLabelPlacement
import org.eclipse.elk.core.options.HierarchyHandling
import org.eclipse.elk.core.util.BasicProgressMonitor
import org.eclipse.elk.graph.ElkEdge
import org.eclipse.elk.graph.ElkNode
import org.eclipse.elk.graph.util.ElkGraphUtil

class ScenarioLayout(scenario: Scenario) {
    val elkGraphRoot: ElkNode = ElkGraphUtil.createGraph()

    private val nodesOrderedByBFS = scenario.bfs()
    private val elkNodeToNodeGroup = createElkNodes(nodesOrderedByBFS, elkGraphRoot)
    private val elkEdgeToChannel = createElkEdges(nodesOrderedByBFS, elkNodeToNodeGroup)

    init {
        setElkContainerNodeProperties(elkGraphRoot)
        /* Code to generate the layout */
        RecursiveGraphLayoutEngine().layout(elkGraphRoot, BasicProgressMonitor())
    }

    fun getNodeGroup(elkNode: ElkNode) = elkNodeToNodeGroup.getValue(elkNode)

    fun getChannel(elkEdge: ElkEdge) = elkEdgeToChannel.getValue(elkEdge)

    private companion object {
        private const val NODE_WIDTH = 80.0
        private const val NODE_HEIGHT = 80.0
        private const val EDGE_LABEL_WIDTH = 60.0
        private const val EDGE_LABEL_HEIGHT = 20.0

        init {
            LayoutMetaDataService.getInstance().registerLayoutMetaDataProviders(LayeredMetaDataProvider())
        }

        private fun createElkNodes(nodes: List<Node>, root: ElkNode): Map<ElkNode, NodeGroup> {
            val nodeGroups = mutableMapOf<NodeGroup, ElkNode>()

            fun getElkNodeFromNodeGroup(nodeGroup: NodeGroup?): ElkNode {
                if (nodeGroup == null) {
                    return root
                }
                if (!nodeGroups.containsKey(nodeGroup)) {
                    val elkNode = ElkGraphUtil.createNode(getElkNodeFromNodeGroup(nodeGroup.parent))
                    elkNode.identifier = nodeGroup.label
                    setElkContainerNodeProperties(elkNode)
                    nodeGroups[nodeGroup] = elkNode
                }
                return nodeGroups[nodeGroup]!!
            }

            return buildMap {
                nodes.associateByTo(this) { createElkNode(it, getElkNodeFromNodeGroup(it.parent)) }
                for ((node, elkNode) in nodeGroups) {
                    put(elkNode, node)
                }
            }
        }

        private fun setElkContainerNodeProperties(elkNode: ElkNode) {
            elkNode.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered")
            elkNode.setProperty(LayeredOptions.NODE_PLACEMENT_STRATEGY, NodePlacementStrategy.BRANDES_KOEPF)
            elkNode.setProperty(LayeredOptions.NODE_PLACEMENT_BK_FIXED_ALIGNMENT, FixedAlignment.BALANCED)
            elkNode.setProperty(LayeredOptions.CONSIDER_MODEL_ORDER_STRATEGY, OrderingStrategy.PREFER_EDGES)
            // the cycle-breaking property must be set in the nodes container because it only applies to its direct
            // children and not grandchildren
            elkNode.setProperty(LayeredOptions.CYCLE_BREAKING_STRATEGY, CycleBreakingStrategy.DFS_NODE_ORDER)
            elkNode.setProperty(LayeredOptions.SPACING_EDGE_NODE_BETWEEN_LAYERS, 15.0)
            elkNode.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.INCLUDE_CHILDREN)
        }

        private fun createElkNode(node: Node, parent: ElkNode?): ElkNode {
            val elkNode = ElkGraphUtil.createNode(parent)
            elkNode.width = NODE_WIDTH
            elkNode.height = NODE_HEIGHT
            elkNode.identifier = node.label
            return elkNode
        }

        private fun createElkEdges(
            nodesOrderedByBFS: List<Node>,
            elkNodeToNodeGroup: Map<ElkNode, NodeGroup>,
        ): Map<ElkEdge, OutputChannel<*, *>> {
            val nodeGroupToElkNode = elkNodeToNodeGroup.entries.associate { it.value to it.key }
            val elkEdgeToChannel = mutableMapOf<ElkEdge, OutputChannel<*, *>>()
            for (source in nodesOrderedByBFS) {
                for (channel in source.outgoing) {
                    val destination = channel.downstream.downstreamNode
                    val edge =
                        ElkGraphUtil.createSimpleEdge(
                            nodeGroupToElkNode.getValue(source),
                            nodeGroupToElkNode.getValue(destination),
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
                    label.width = EDGE_LABEL_WIDTH
                    label.height = EDGE_LABEL_HEIGHT
                    elkEdgeToChannel[edge] = channel
                }
            }
            return elkEdgeToChannel
        }
    }
}
