package com.group7.properties

import com.group7.DisplayProperty
import com.group7.FieldDisplayProperty
import com.group7.HasDisplayProperties
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

interface BoundedContainer<out T> : Container<T>, HasDisplayProperties {
    val capacity: Int

    val isFull
        get() = occupants >= capacity

    val utilisation
        get() = occupants.toDouble() / capacity

    override fun properties(): List<DisplayProperty> = listOf(FieldDisplayProperty("Capacity", "$capacity"))
}
