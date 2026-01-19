package com.group7.nodes

import com.group7.InputChannel
import com.group7.Node
import com.group7.OutputChannel
import com.group7.Simulator

class SplitNode<T, A, B>(
    label: String,
    source: InputChannel<T>,
    private val destinationA: OutputChannel<A>,
    private val destinationB: OutputChannel<B>,
    private val splitter: (T) -> Pair<A, B>,
) : Node(label, listOf(destinationA, destinationB)) {

    init {
        source.onReceive { onArrive(it) }
    }

    context(_: Simulator)
    private fun onArrive(obj: T) {
        val (a, b) = splitter(obj)
        destinationA.send(a)
        destinationB.send(b)
    }
}
