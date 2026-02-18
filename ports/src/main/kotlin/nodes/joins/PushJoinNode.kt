package com.group7.nodes.joins

import com.group7.Node
import com.group7.TextDisplayProperty
import com.group7.channels.*

/** Joins multiple streams together. */
class PushJoinNode<T>(label: String, sources: List<PushInputChannel<T>>, destination: PushOutputChannel<T>) :
    Node(label, sources, listOf(destination)) {

    init {
        for (source in sources) {
            source.onReceive { destination.send(it) }
        }

        destination.whenOpened { sources.forEach { it.open() } }
        destination.whenClosed { sources.forEach { it.close() } }
    }

    override fun properties() = listOf(TextDisplayProperty("Push join node"))
}
