package com.group7

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

object GroupScope {
    private val _current = ThreadLocal.withInitial<NodeGroup?> { null }

    var current: NodeGroup?
        get() = _current.get()
        set(value) {
            _current.set(value)
        }

    inline fun <T> withGroup(group: NodeGroup?, block: () -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        val old = current
        current = group
        try {
            return block()
        } finally {
            current = old
        }
    }
}
