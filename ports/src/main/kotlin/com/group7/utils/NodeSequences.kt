package com.group7.utils

import com.group7.NodeGroup
import com.group7.channels.InputChannel
import com.group7.channels.OutputChannel

/**
 * Walks downstream from this OutputChannel, and returns a sequence of the following [NodeGroup] instances that exist
 * downstream of this channel.
 */
fun OutputChannel<*, *>.walkDownstream(): Sequence<NodeGroup> = sequence {
    var current = this@walkDownstream
    while (true) {
        var next: NodeGroup = current.downstream.downstreamNode
        val currentGroup = current.upstreamNode.parent
        when (val nextGroup = next.parent) {
            currentGroup -> {
                // Same group, carry on
            }
            in currentGroup -> {
                // The new group is strictly contained within the current group, so it can't be null
                // Treat the group as a whole
                next = nextGroup!!
            }
            else -> {
                // Exit this group but carry on
            }
        }
        yield(next)
        current = next.outgoing.singleOrNull() ?: break
    }
}

/**
 * Walks upstream from this InputChannel, and returns a sequence of the following [NodeGroup] instances that exist
 * upstream of this channel.
 */
fun InputChannel<*, *>.walkUpstream(): Sequence<NodeGroup> = sequence {
    var current = this@walkUpstream
    while (true) {
        var next: NodeGroup = current.upstream.upstreamNode
        val currentGroup = current.downstreamNode.parent
        when (val nextGroup = next.parent) {
            currentGroup -> {
                // Same group, carry on
            }
            in currentGroup -> {
                // The new group is strictly contained within the current group, so it can't be null
                // Treat the group as a whole
                next = nextGroup!!
            }
            else -> {
                // Exit this group but carry on
            }
        }
        yield(next)
        current = next.incoming.singleOrNull() ?: break
    }
}

/**
 * Returns whether this `NodeGroup` wholely contains `other` NodeGroup
 *
 * @param other NodeGroup potentially contained within this NodeGroup
 */
private tailrec operator fun NodeGroup?.contains(other: NodeGroup?): Boolean =
    when {
        this == other || this == null -> true
        other == null -> false
        else -> other.parent in this
    }
