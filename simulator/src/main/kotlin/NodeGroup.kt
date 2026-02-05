package com.group7

import com.group7.channels.InputChannel
import com.group7.channels.OutputChannel

abstract class NodeGroup(val label: String) {
    var parent: NodeGroup? = null

    abstract val incoming: List<InputChannel<*, *>>

    abstract val outgoing: List<OutputChannel<*, *>>

    override fun toString() = label
}
