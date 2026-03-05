package com.group7.nodes.forks

import com.group7.Node
import com.group7.channels.*

/**
 * Distributes entities from a single pull input to multiple pull outputs. Each output independently pulls from the
 * source when needed.
 *
 * @param T the type of entities being distributed
 * @param label the name of this node
 * @param source the input channel from which entities are pulled
 * @param destinations the list of output channels to distribute entities to
 */
class PullForkNode<T>(
    label: String,
    private val source: PullInputChannel<T>,
    private val destinations: List<PullOutputChannel<T>>,
) : Node(label, listOf(source), destinations) {
    init {
        source.whenReady { destinations.forEach { it.markReady() } }
        source.whenNotReady { destinations.forEach { it.markNotReady() } }

        destinations.forEach { it.onPull { source.receive() } }
    }
}
