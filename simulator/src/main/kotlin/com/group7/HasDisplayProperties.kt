package com.group7

/**
 * Interface for objects that can provide display properties for UI rendering.
 *
 * Nodes and other simulation objects can implement this interface to supply display properties that will be shown in
 * the user interface.
 */
interface HasDisplayProperties {
    /**
     * Returns the display properties for this object.
     *
     * @return a list of [DisplayProperty] objects to display, or empty if no properties to display
     */
    fun properties(): List<DisplayProperty> = emptyList()
}
