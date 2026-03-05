package com.group7.tags

import com.group7.channels.PullInputChannel
import com.group7.channels.PushOutputChannel

/*
Currently used for forward walks to find containers following a fork node
 */
sealed interface OutputTag<out T> {
    fun find(start: PushOutputChannel<*>): T
}

/*
Currently used for backward walks to find containers supplying a join node
 */
sealed interface InputTag<out T> {
    fun find(start: PullInputChannel<*>): T
}

sealed interface BasicTag<out T> : OutputTag<T>, InputTag<T> {
    val value: T

    override fun find(start: PushOutputChannel<*>): T = value

    override fun find(start: PullInputChannel<*>): T = value
}

sealed interface MutableBasicTag<T> : BasicTag<T> {
    fun bind(obj: T)
}

fun <T> newTag(): MutableBasicTag<T> = BasicTagImpl()

fun <T> newTag(value: T): BasicTag<T> = BasicTagImpl(value)

fun <T> newDynamicOutputTag(supplier: (PushOutputChannel<*>) -> T): OutputTag<T> = DynamicOutputTagImpl(supplier)

fun <T> newDynamicInputTag(supplier: (PullInputChannel<*>) -> T): InputTag<T> = DynamicInputTagImpl(supplier)

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

private class DynamicOutputTagImpl<out T>(private val supplier: (PushOutputChannel<*>) -> T) : OutputTag<T> {
    private var previousStart: PushOutputChannel<*>? = null
    private var cached: T? = null

    override fun find(start: PushOutputChannel<*>): T {
        if (start === previousStart) {
            @Suppress("UNCHECKED_CAST")
            return cached as T
        }
        previousStart = start
        return supplier(start).also { cached = it }
    }
}

private class DynamicInputTagImpl<out T>(private val supplier: (PullInputChannel<*>) -> T) : InputTag<T> {
    private var previousStart: PullInputChannel<*>? = null
    private var cached: T? = null

    override fun find(start: PullInputChannel<*>): T {
        if (start === previousStart) {
            @Suppress("UNCHECKED_CAST")
            return cached as T
        }
        previousStart = start
        return supplier(start).also { cached = it }
    }
}
