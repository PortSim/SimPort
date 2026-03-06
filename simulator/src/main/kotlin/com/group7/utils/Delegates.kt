package com.group7.utils

import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Creates a delegate property that can only be set once, and must be set before reading.
 *
 * Useful for properties that must be initialized exactly once during setup but cannot be initialized in the
 * constructor.
 *
 * @return a read-write property delegate that enforces single initialization
 */
@Suppress("UnusedReceiverParameter") fun <T : Any> Delegates.setOnce(): ReadWriteProperty<Any?, T> = SetOnce()

/**
 * Internal implementation of the set-once delegate.
 *
 * Raises [IllegalStateException] if attempting to read before initialization, or attempting to set the value more than
 * once.
 */
private class SetOnce<T : Any> : ReadWriteProperty<Any?, T> {
    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>) =
        value ?: throw IllegalStateException("${property.name} should be initialized before get.")

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        check(this.value == null) { "${property.name} is already initialized." }
        this.value = value
    }
}
