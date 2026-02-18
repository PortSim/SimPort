package com.group7.properties

import com.group7.DisplayProperty

interface HasDisplayProperties {
    fun properties(): List<DisplayProperty> = emptyList()
}
