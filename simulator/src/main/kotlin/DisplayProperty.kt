package com.group7

sealed interface DisplayProperty

class GroupDisplayProperty(val name: String, val list: List<DisplayProperty>) : DisplayProperty {
    constructor(name: String) : this(name, emptyList())

    constructor(name: String, vararg properties: DisplayProperty) : this(name, properties.toList())

    fun takeChildrenOf(other: GroupDisplayProperty): GroupDisplayProperty {
        check(other.name == name)
        return GroupDisplayProperty(name, list + other.list)
    }

    fun addChild(other: DisplayProperty): GroupDisplayProperty {
        return GroupDisplayProperty(name, list + other)
    }
}

class OccupantsDisplayProperty(val label: String, val occupants: Int, val capacity: Int?) : DisplayProperty

class DoubleDisplayProperty(val label: String, val value: Double, val unitSuffix: String) : DisplayProperty

class TextDisplayProperty(val label: String) : DisplayProperty

// some way for the user to just print whatever onto the display if they want ?? class Arbitrary()
