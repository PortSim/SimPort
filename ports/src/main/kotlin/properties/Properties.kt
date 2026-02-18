package com.group7.properties

import com.group7.DisplayProperty
import com.group7.FieldDisplayProperty
import com.group7.Simulator

interface Container<out T> : HasDisplayProperties {
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

    override fun properties(): List<DisplayProperty> = listOf(FieldDisplayProperty("Occupants", "$occupants"))
}

interface BoundedContainer<out T> : Container<T>, HasDisplayProperties {
    val capacity: Int

    val isFull
        get() = occupants >= capacity

    override fun properties(): List<DisplayProperty> =
        listOf(FieldDisplayProperty("Occupants", "$occupants / $capacity"))
}
