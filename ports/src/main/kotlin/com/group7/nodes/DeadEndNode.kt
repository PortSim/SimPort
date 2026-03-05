package com.group7.nodes

import com.group7.Node
import com.group7.Simulator
import com.group7.channels.InputChannel
import com.group7.channels.close
import com.group7.channels.isPush

/**
 * Dead end node closes its input channel immediately, representing a queue node that is 'closed for business', and will
 * crash the simulator if a vehicle is dispatched to this node.
 */
class DeadEndNode<InputT>(label: String, private val inputChannel: InputChannel<InputT, *>) :
    Node(label, listOf(inputChannel), emptyList()) {

    context(_: Simulator)
    override fun onStart() {
        if (inputChannel.isPush()) {
            inputChannel.close()
        }
    }
}
