package com.group7

import com.group7.properties.BoundedContainer
import com.group7.properties.Container

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

fun NodeGroup.reportOccupants() =
    Occupants(occupants = (this as? Container<*>)?.occupants, capacity = (this as? BoundedContainer<*>)?.capacity)
