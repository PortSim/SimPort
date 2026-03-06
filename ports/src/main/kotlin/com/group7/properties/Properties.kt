package com.group7.properties

import com.group7.Simulator

/*
Properties are inherited by nodes with these properties, and thus must have methods for Metrics to link into
 */

/**
 * Indicates a node is an unbounded container with no maximum capacity.
 *
 * @property occupants The number of occupants held by the container
 * @property onEnter Callback invoked by the node when an entity enters the container
 * @property onLeave Callback invoked by the node when an entity leaves the container
 */
interface Container<out T> {
    val occupants: Int

    /** Callback to execute when an entity enters the container */
    fun onEnter(
        callback:
            context(Simulator)
            (T) -> Unit
    )

    /** Callback to execute when an entity leaves the container */
    fun onLeave(
        callback:
            context(Simulator)
            (T) -> Unit
    )

    // By default, all containers support residence time, as entities that enter should eventually leave
    fun supportsResidenceTime(): Boolean = true
}

/**
 * Indicates a node is a bounded container with some maximum capacity defined.
 *
 * @property capacity The maximum capacity of the container
 * @property isFull Whether the container is at max occupancy
 * @property utilisation Percentage of how much of the container's max capacity is used at the moment
 */
interface BoundedContainer<out T> : Container<T>, HasDisplayProperties {
    val capacity: Int

    val isFull
        get() = occupants >= capacity

    val utilisation
        get() = occupants.toDouble() / capacity

    override fun properties(): List<DisplayProperty> = listOf(FieldDisplayProperty("Capacity", "$capacity"))
}
