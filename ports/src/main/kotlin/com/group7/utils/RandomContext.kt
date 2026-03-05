package com.group7.utils

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.random.Random

object RandomContext {
    private val _metaRandom = ThreadLocal.withInitial { Random(System.nanoTime()) }

    @PublishedApi
    internal var metaRandom
        get() = _metaRandom.get()
        set(value) = _metaRandom.set(value)

    fun newRandom() = Random(nextLong())

    fun nextLong() = metaRandom.nextLong()

    inline fun <T> withSeed(seed: Long, block: () -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        val oldRandom = metaRandom
        metaRandom = Random(seed)
        try {
            return block()
        } finally {
            metaRandom = oldRandom
        }
    }
}
