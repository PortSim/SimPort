package com.group7

import com.group7.channels.InputChannel
import com.group7.channels.OutputChannel

/**
 * Base class representing a group of nodes or individual node in the simulation network.
 *
 * A node group can be a single [Node] or a hierarchical grouping of multiple nodes. It maintains references to its
 * incoming and outgoing channels and supports display properties.
 *
 * @property label human-readable name for this node group
 */
abstract class NodeGroup(val label: String) : HasDisplayProperties {
    /**
     * The parent node group, if this group was created within another group's scope.
     *
     * Allows for hierarchical organization of nodes.
     */
    val parent = GroupScope.current

    /**
     * Stack trace at the point this node group was created.
     *
     * Useful for debugging to identify where in the code a node was instantiated.
     */
    val stackTrace = Thread.currentThread().stackTrace.drop(1)

    /** All input channels connected to this node group. */
    abstract val incoming: List<InputChannel<*, *>>

    /** All output channels connected to this node group. */
    abstract val outgoing: List<OutputChannel<*, *>>

    /** Prints the label of the [NodeGroup] */
    override fun toString() = label
}
