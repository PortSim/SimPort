package com.group7

abstract class NodeGroup(val label: String) {
    var parent: NodeGroup? = null

    abstract val outgoing: List<OutputChannel<*>>

    override fun toString() = label
}
