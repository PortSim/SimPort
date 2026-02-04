package com.group7.compound

import com.group7.NodeGroup
import com.group7.channels.InputChannel
import com.group7.channels.OutputChannel
import com.group7.dsl.Connection
import com.group7.dsl.GroupScope
import com.group7.dsl.OutputRef
import com.group7.dsl.nextInput

abstract class CompoundNode(label: String, incoming: List<Connection<*, *>>, outgoing: List<OutputRef<*, *>>) :
    NodeGroup(label), GroupScope {
    final override val incoming: List<InputChannel<*, *>> = incoming.map { it.nextInput }
    final override val outgoing: List<OutputChannel<*, *>> by lazy { outgoing.map { it.output } }

    final override val group
        get() = this
}
