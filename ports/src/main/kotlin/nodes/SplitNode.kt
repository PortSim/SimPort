package com.group7.nodes

import com.group7.Node
import com.group7.Simulator
import com.group7.channels.*

class SplitNode<T, A, B>(
    label: String,
    source: PushInputChannel<T>,
    private val destinationA: PushOutputChannel<A>,
    private val destinationB: PushOutputChannel<B>,
    private val splitter: (T) -> Pair<A, B>,
) : Node(label, listOf(source), listOf(destinationA, destinationB)) {

    init {
        source.onReceive { onArrive(it) }

        destinationA.whenClosed { source.close() }
        destinationB.whenClosed { source.close() }

        destinationA.whenOpened {
            if (destinationB.isOpen()) {
                source.open()
            }
        }
        destinationB.whenOpened {
            if (destinationA.isOpen()) {
                source.open()
            }
        }
    }

    context(_: Simulator)
    private fun onArrive(obj: T) {
        val (a, b) = splitter(obj)
        destinationA.send(a)
        destinationB.send(b)
    }
}
