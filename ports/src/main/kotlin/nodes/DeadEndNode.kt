package com.group7.nodes

import com.group7.InputChannel
import com.group7.Node
import com.group7.Simulator

/**
 * Dead end node closes its input channel immediately, representing a queue node that is 'closed for business', and will
 * crash the simulator if a vehicle is dispatched to this node.
 */
class DeadEndNode<T>(label: String, private val inputChannel: InputChannel<T>) : Node(label, emptyList()) {
    init {
        inputChannel.onReceive {}
    }

    context(_: Simulator)
    override fun onStart() {
        inputChannel.close()
    }
}
