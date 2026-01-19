package com.group7.nodes

import com.group7.InputChannel
import com.group7.Node
import com.group7.OutputChannel

class JoinNode<T>(label: String, sources: List<InputChannel<T>>, destination: OutputChannel<T>) :
    Node(label, listOf(destination)) {

    init {
        for (source in sources) {
            source.onReceive { destination.send(it) }
        }

        destination.whenOpened { sources.forEach { it.open() } }
        destination.whenClosed { sources.forEach { it.close() } }
    }
}
