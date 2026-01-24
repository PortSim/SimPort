package com.group7.nodes

import com.group7.InputChannel
import com.group7.Node
import com.group7.OutputChannel
import com.group7.Simulator

class MatchNode<A, B, R>(
    label: String,
    private val sourceA: InputChannel<A>,
    private val sourceB: InputChannel<B>,
    private val destination: OutputChannel<R>,
    private val combiner: (A, B) -> R,
) : Node(label, listOf(destination)) {

    private var valueA: Value<A>? = null
    private var valueB: Value<B>? = null

    init {
        sourceA.onReceive { onArriveA(it) }
        sourceB.onReceive { onArriveB(it) }

        destination.whenOpened {
            // Only reopen sourceA if we want something from it
            if (valueA == null) {
                sourceA.open()
            }
            // Only reopen sourceB if we want something from it
            if (valueB == null) {
                sourceB.open()
            }
        }

        destination.whenClosed {
            sourceA.close()
            sourceB.close()
        }
    }

    context(_: Simulator)
    private fun onArriveA(a: A) {
        valueB?.let { b ->
            // Can emit immediately
            valueB = null
            sourceB.open()
            emit(a, b.value)
            return
        }
        // Need to wait for B
        valueA = Value(a)
        sourceA.close()
    }

    context(_: Simulator)
    private fun onArriveB(b: B) {
        valueA?.let { a ->
            // Can emit immediately
            valueA = null
            sourceA.open()
            emit(a.value, b)
            return
        }
        // Need to wait for A
        valueB = Value(b)
        sourceB.close()
    }

    context(_: Simulator)
    private fun emit(a: A, b: B) {
        destination.send(combiner(a, b))
    }

    private data class Value<T>(val value: T)
}
