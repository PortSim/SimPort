package com.group7.tags

import com.group7.OutputChannel

sealed interface Tag<out T> {
    fun find(start: OutputChannel<*>): T
}

sealed interface BasicTag<out T> : Tag<T> {
    val value: T

    override fun find(start: OutputChannel<*>): T = value
}

sealed interface MutableBasicTag<T> : BasicTag<T> {
    fun bind(obj: T)
}

fun <T> newTag(): MutableBasicTag<T> = BasicTagImpl()

fun <T> newTag(value: T): BasicTag<T> = BasicTagImpl(value)

fun <T> newDynamicTag(supplier: (OutputChannel<*>) -> T): Tag<T> = DynamicTagImpl(supplier)

private class BasicTagImpl<T>() : MutableBasicTag<T> {
    private var isBound = false
    private var _value: T? = null

    constructor(value: T) : this() {
        isBound = true
        _value = value
    }

    override val value: T
        get() {
            check(isBound) { "Tag is not bound" }
            @Suppress("UNCHECKED_CAST")
            return _value as T
        }

    override fun bind(obj: T) {
        check(!isBound) { "Tag is already bound" }
        _value = obj
        isBound = true
    }
}

private class DynamicTagImpl<out T>(private val supplier: (OutputChannel<*>) -> T) : Tag<T> {
    private var previousStart: OutputChannel<*>? = null
    private var cached: T? = null

    override fun find(start: OutputChannel<*>): T {
        if (start === previousStart) {
            @Suppress("UNCHECKED_CAST")
            return cached as T
        }
        previousStart = start
        return supplier(start).also { cached = it }
    }
}
