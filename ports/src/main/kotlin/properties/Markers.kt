package com.group7.properties

import com.group7.OccupantsDisplayProperty
import com.group7.Simulator

interface Source<out T> {
    fun onEmit(
        callback:
            context(Simulator)
            (T) -> Unit
    )
}

interface Delay<out T> : Container<T>

interface Match<out MainInputT, out SideInputT, out OutputT> {
    fun onMatch(
        callback:
            context(Simulator)
            (MainInputT, SideInputT, OutputT) -> Unit
    )
}

interface Queue<out T> : Container<T>

interface Service<out T> : BoundedContainer<T> {
    val isServing: Boolean

    override val occupants
        get() = if (isServing) 1 else 0

    override val capacity
        get() = 1
}

fun <T> Service<T>.asCapacityDisplayProperty() = OccupantsDisplayProperty("Occupancy", occupants, capacity)

interface Sink<out T> : Container<T> {
    override fun onLeave(
        callback:
            context(Simulator)
            (T) -> Unit
    ) {}

    override fun supportsResidenceTime() = false
}

interface Split<out InputT, out MainOutputT, out SideOutputT> {
    fun onSplit(
        callback:
            context(Simulator)
            (InputT, MainOutputT, SideOutputT) -> Unit
    )
}
