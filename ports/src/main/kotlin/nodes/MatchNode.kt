package com.group7.nodes

import com.group7.Node
import com.group7.Simulator
import com.group7.channels.PushInputChannel
import com.group7.channels.PushOutputChannel
import com.group7.properties.Match

class MatchNode<A, B, R>(
    label: String,
    private val sourceA: PushInputChannel<A>,
    private val sourceB: PushInputChannel<B>,
    private val destination: PushOutputChannel<R>,
    private val combiner: (A, B) -> R,
) : Node(label, listOf(destination)), Match {

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

    override val hasLeft
        get() = valueA != null

    override val hasRight
        get() = valueB != null

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
