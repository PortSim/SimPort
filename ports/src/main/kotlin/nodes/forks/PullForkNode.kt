package com.group7.nodes.forks

import com.group7.Node
import com.group7.TextDisplayProperty
import com.group7.channels.*

class PullForkNode<T>(
    label: String,
    private val source: PullInputChannel<T>,
    private val destinations: List<PullOutputChannel<T>>,
) : Node(label, listOf(source), destinations) {
    init {
        source.whenReady { destinations.forEach { it.markReady() } }
        source.whenNotReady { destinations.forEach { it.markNotReady() } }

        destinations.forEach { it.onPull { source.receive() } }
    }

    override fun properties() = listOf(TextDisplayProperty("Pull fork node"))
}
