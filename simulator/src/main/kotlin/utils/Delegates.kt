package com.group7.utils

import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Suppress("UnusedReceiverParameter") fun <T : Any> Delegates.setOnce(): ReadWriteProperty<Any?, T> = SetOnce()

private class SetOnce<T : Any> : ReadWriteProperty<Any?, T> {
    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>) =
        value ?: throw IllegalStateException("${property.name} should be initialized before get.")

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        check(this.value == null) { "${property.name} is already initialized." }
        this.value = value
    }
}
