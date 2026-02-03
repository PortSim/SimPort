package com.group7.nodes

import com.group7.Node
import com.group7.channels.PushInputChannel
import com.group7.channels.PushOutputChannel

/** Joins multiple streams together. */
class JoinNode<T>(label: String, sources: List<PushInputChannel<T>>, destination: PushOutputChannel<T>) :
    Node(label, listOf(destination)) {

    init {
        for (source in sources) {
            source.onReceive { destination.send(it) }
        }

        destination.whenOpened { sources.forEach { it.open() } }
        destination.whenClosed { sources.forEach { it.close() } }
    }
}
