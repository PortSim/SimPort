package com.group7

import com.group7.metrics.MetricGroup

/**
 * Base interface for properties that can be displayed in the user interface.
 *
 * Different display property implementations allow nodes to present various types of information in the simulation UI.
 */
sealed interface DisplayProperty

/**
 * A grouping container for display properties, with a hierarchical structure.
 *
 * Allows organizing related properties under a named group.
 *
 * @property name the name of this group
 * @property list the child properties contained in this group
 */
class GroupDisplayProperty(val name: String, val list: List<DisplayProperty>) : DisplayProperty {
    /**
     * Creates an empty group with no children.
     *
     * @param name the name of this group
     */
    constructor(name: String) : this(name, emptyList())

    /**
     * Creates a group with the specified children.
     *
     * @param name the name of this group
     * @param properties variable arguments of child properties
     */
    constructor(name: String, vararg properties: DisplayProperty) : this(name, properties.toList())

    /**
     * Adds a child property to this group.
     *
     * @param other the child [DisplayProperty] to add
     * @return a new [GroupDisplayProperty] with the child added
     */
    fun addChild(other: DisplayProperty): GroupDisplayProperty {
        return GroupDisplayProperty(name, list + other)
    }
}

/**
 * A display property that references a [MetricGroup] for display.
 *
 * @property metricGroup the [MetricGroup] to display
 */
class MetricGroupDisplayProperty(val metricGroup: MetricGroup) : DisplayProperty

/**
 * A display property that shows a line of text in the UI sidepanel.
 *
 * @property string the text to display
 */
class TextDisplayProperty(val string: String) : DisplayProperty

/**
 * A display property that shows a numerical value with optional units.
 *
 * @property label the label for this value
 * @property value the numerical value to display
 * @property unitSuffix a suffix to display after the value (e.g., "ms", "items")
 */
class DoubleDisplayProperty(val label: String, val value: Double, val unitSuffix: String) : DisplayProperty

/**
 * A display property that shows a "field: value" pair with left-aligned field and right-aligned value.
 *
 * @property fieldName the name of the field
 * @property value the value to display for this field
 */
class FieldDisplayProperty(val fieldName: String, val value: String) : DisplayProperty
