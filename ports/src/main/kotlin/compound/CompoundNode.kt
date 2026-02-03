package com.group7.compound

import com.group7.NodeGroup
import com.group7.dsl.GroupScope
import com.group7.dsl.OutputRef

abstract class CompoundNode(label: String, outgoing: List<OutputRef<*>>) : NodeGroup(label), GroupScope {
    final override val outgoing by lazy { outgoing.map { it.output } }

    final override val group
        get() = this
}
