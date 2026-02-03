package com.group7.properties

interface Container {
    val occupants: Int
}

interface BoundedContainer : Container {
    val capacity: Int

    val isFull
        get() = occupants >= capacity
}
