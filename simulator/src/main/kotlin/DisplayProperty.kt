package com.group7

import com.group7.metrics.MetricGroup

sealed interface DisplayProperty

class GroupDisplayProperty(val name: String, val list: List<DisplayProperty>) : DisplayProperty {
    constructor(name: String) : this(name, emptyList())

    constructor(name: String, vararg properties: DisplayProperty) : this(name, properties.toList())

    fun addChild(other: DisplayProperty): GroupDisplayProperty {
        return GroupDisplayProperty(name, list + other)
    }
}

class MetricGroupDisplayProperty(val metricGroup: MetricGroup) : DisplayProperty

/* Displays a line of text in the sidepanel */
class TextDisplayProperty(val string: String) : DisplayProperty

class DoubleDisplayProperty(val label: String, val value: Double, val unitSuffix: String) : DisplayProperty

/* Displays a "field: value" where field is left aligned and value is right aligned */
class FieldDisplayProperty(val fieldName: String, val value: String) : DisplayProperty
