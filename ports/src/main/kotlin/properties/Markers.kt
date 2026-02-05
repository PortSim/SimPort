package com.group7.properties

interface Delay : Container

interface Queue : Container

interface Service : BoundedContainer {
    val isServing: Boolean

    override val occupants
        get() = if (isServing) 1 else 0

    override val capacity
        get() = 1
}

interface Sink : Container

interface Match : BoundedContainer {
    val hasLeft: Boolean
    val hasRight: Boolean

    override val occupants
        get() = if (hasLeft || hasRight) 1 else 0

    override val capacity
        get() = 1
}
