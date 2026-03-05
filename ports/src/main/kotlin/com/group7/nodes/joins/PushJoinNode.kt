package com.group7.nodes.joins

import com.group7.Node
import com.group7.channels.*

/**
 * Merges entities from multiple push inputs into a single push output. Any entity arriving from any source is
 * immediately forwarded to the destination.
 *
 * @param T the type of entities being merged
 * @param label the name of this node
 * @param sources the list of input channels to merge from
 * @param destination the output channel where merged entities are sent
 */
class PushJoinNode<T>(label: String, sources: List<PushInputChannel<T>>, destination: PushOutputChannel<T>) :
    Node(label, sources, listOf(destination)) {

    init {
        for (source in sources) {
            source.onReceive { destination.send(it) }
        }

        destination.whenOpened { sources.forEach { it.open() } }
        destination.whenClosed { sources.forEach { it.close() } }
    }
}
