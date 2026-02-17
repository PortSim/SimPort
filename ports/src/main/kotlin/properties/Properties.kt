package com.group7.properties

import com.group7.Simulator

interface Container<out T> {
    val occupants: Int

    fun onEnter(
        callback:
            context(Simulator)
            (T) -> Unit
    )

    fun onLeave(
        callback:
            context(Simulator)
            (T) -> Unit
    )

    fun supportsResidenceTime(): Boolean = true
}

interface BoundedContainer<out T> : Container<T> {
    val capacity: Int

    val isFull
        get() = occupants >= capacity
}
