package com.group7

class Port(val nodes: List<Node<*, *, *>>) {
    constructor(vararg nodes: Node<*, *, *>) : this(nodes.toList())
}
