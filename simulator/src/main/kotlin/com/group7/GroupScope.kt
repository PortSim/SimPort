package com.group7

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Thread-local context manager for hierarchical node grouping.
 *
 * Allows nodes to be created within the scope of a parent node group, establishing a hierarchical organization.
 */
object GroupScope {
    private val _current = ThreadLocal.withInitial<NodeGroup?> { null }

    /** The currently active node group, or null if not within a group scope. */
    var current: NodeGroup?
        get() = _current.get()
        set(value) {
            _current.set(value)
        }

    /**
     * Executes a block of code within the scope of a node group.
     *
     * Any [NodeGroup] created within the block will have their parent set to the given group. The scope is
     * automatically restored when the block completes, even if an exception occurs.
     *
     * @param group the [NodeGroup] to make the current scope, or null to clear the scope
     * @param block the code block to execute
     * @return the result of executing the block
     */
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
