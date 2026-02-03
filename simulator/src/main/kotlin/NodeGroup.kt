package com.group7

import com.group7.channels.PushOutputChannel

abstract class NodeGroup(val label: String) {
    var parent: NodeGroup? = null

    abstract val outgoing: List<PushOutputChannel<*>>

    override fun toString() = label
}
