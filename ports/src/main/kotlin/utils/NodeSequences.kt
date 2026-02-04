package com.group7.utils

import com.group7.NodeGroup
import com.group7.channels.OutputChannel

fun OutputChannel<*, *>.walkDownstream() = sequence {
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

private tailrec operator fun NodeGroup?.contains(other: NodeGroup?): Boolean =
    when {
        this == other || this == null -> true
        other == null -> false
        else -> other.parent in this
    }
