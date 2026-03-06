package com.group7.tags

import com.group7.channels.PullInputChannel
import com.group7.channels.PushOutputChannel

/*
Currently, tags are used for traversals for policies that are identifying, but tags are designed to be able to identify
nodes of interest within a scenario
 */

/**
 * A tag for some identifiable resource that can be identified by following [PushOutputChannel] (forward walk)
 *
 * Currently used for forward walks to find containers following a fork node
 */
sealed interface OutputTag<out T> {
    fun find(start: PushOutputChannel<*>): T
}

/**
 * A tag for some identifiable resource that can be identified by following [PullInputChannel] (backward walk)
 *
 * Currently used for backward walks to find containers supplying a join node
 */
sealed interface InputTag<out T> {
    fun find(start: PullInputChannel<*>): T
}

/**
 * A tag for some resource, that can be identified by both backward and forward walk through the network.
 *
 * @property value The resource associated with the tag
 */
sealed interface BasicTag<out T> : OutputTag<T>, InputTag<T> {
    val value: T

    // A forward walk that reaches this tag, so we should return the value stored at this tag
    override fun find(start: PushOutputChannel<*>): T = value

    // A backward walk that reaches this tag, so we should return the value stored at this tag
    override fun find(start: PullInputChannel<*>): T = value
}

/** A basic tag that can be bound to the resource later after creation. */
sealed interface MutableBasicTag<T> : BasicTag<T> {
    fun bind(obj: T)
}

/**
 * Returns a [MutableBasicTag] instance
 *
 * @param T Type of resource to be associated with this [MutableBasicTag] later
 */
fun <T> newTag(): MutableBasicTag<T> = BasicTagImpl()

/**
 * Returns a [BasicTag] instance
 *
 * @param T Type of `value` to be associated with this tag
 * @param value Value to associate the tag with
 */
fun <T> newTag(value: T): BasicTag<T> = BasicTagImpl(value)

/**
 * Returns the [OutputTag] associated with the resource identified.
 *
 * @param T Type of resource to be identified by the `supplier`
 * @param supplier Lambda function to identify the resource to be tagged, given some starting [PushOutputChannel]
 */
fun <T> newDynamicOutputTag(supplier: (PushOutputChannel<*>) -> T): OutputTag<T> = DynamicOutputTagImpl(supplier)

/**
 * Returns the [InputTag] associated with the resource identified.
 *
 * @param T Type of resource to be identified by the `supplier`
 * @param supplier Lambda function to identify the resource to be tagged, given some starting [PullInputChannel]
 */
fun <T> newDynamicInputTag(supplier: (PullInputChannel<*>) -> T): InputTag<T> = DynamicInputTagImpl(supplier)

/**
 * Internal implementation of [MutableBasicTag]
 *
 * Does not need to be bound to a resource initially, but can be created with the resource identified already.
 */
private class BasicTagImpl<T>() : MutableBasicTag<T> {
    private var isBound = false
    private var _value: T? = null

    /** Given the resource upon creation, this tag will behave like a [BasicTag] */
    constructor(value: T) : this() {
        isBound = true
        _value = value
    }

    /**
     * The value associated with this tag.
     *
     * @throws IllegalStateException if tag is not bound to anything yet
     */
    override val value: T
        get() {
            check(isBound) { "Tag is not bound" }
            @Suppress("UNCHECKED_CAST")
            return _value as T
        }

    /**
     * Binds provided object to this tag, if this tag is not already bound.
     *
     * @param obj Resource to be associated with the tag
     * @throws IllegalStateException if tag is already bound
     */
    override fun bind(obj: T) {
        check(!isBound) { "Tag is already bound" }
        _value = obj
        isBound = true
    }
}

/**
 * Internal implementation of [OutputTag]
 *
 * @param supplier Lambda function to identify the resource to be tagged, given a starting [PushOutputChannel]
 */
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

/**
 * Internal implementation of [InputTag]
 *
 * @param supplier Lambda function to identify the resource to be tagged, given a starting [PullInputChannel]
 */
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
