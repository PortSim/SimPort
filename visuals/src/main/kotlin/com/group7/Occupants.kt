package com.group7

import com.group7.properties.BoundedContainer
import com.group7.properties.Container

/**
 * Represents the occupancy information for a node.
 *
 * @property occupants the current number of entities in the node, or null if not a container
 * @property capacity the maximum capacity of the node, or null if unbounded or not a container
 */
data class Occupants(val occupants: Int?, val capacity: Int?) {
    override fun toString() = buildString {
        if (occupants != null) {
            append("Occ: $occupants")
            if (capacity != null) {
                append("/$capacity")
            }
        }
    }
}

/**
 * Extracts the occupancy information from a node group.
 *
 * @return Occupants data extracted from this node if it's a container, otherwise null values
 */
fun NodeGroup.reportOccupants() =
    Occupants(occupants = (this as? Container<*>)?.occupants, capacity = (this as? BoundedContainer<*>)?.capacity)
