package com.group7

import kotlin.time.Duration
import umontreal.ssj.rng.MRG32k3a
import umontreal.ssj.rng.RandomStream
import umontreal.ssj.randvar.ExponentialGen
import kotlin.time.Duration.Companion.seconds

// A generator interface used only by Source nodes
interface Generator<OutputT> {
    // A function the source can call to get a delay when scheduling the next emit event
    fun nextDelay(): Duration
    // A function the source can call to get the next object to emit
    fun nextObject(): OutputT
    // If there are any objects left for the generator to generate
    val empty: Boolean
}

// A generator that generates `count` objects with a delay of `delay` between each
class RepetitiveGenerator<OutputT>(
    private val obj: OutputT,
    private val delay: Duration,
    private var count: UInt?, // null means emit forever
) : Generator<OutputT> {
    override fun nextDelay() = delay
    override fun nextObject(): OutputT {
        if (count != null) {
            check(count!! >= 0u)
            count = count!! - 1u
        }
        return obj
    }
    override var empty = count != null && count!! <= 0u
}

class ExponentialGenerator<OutputT>(
    private val obj: OutputT, // object to always output
    lambda: Double, // parameter for the exp distribution
    private var count: UInt?, // null means emit forever
) : Generator<OutputT> {
    private val stream: RandomStream = MRG32k3a() // random number stream
    private val expGen = ExponentialGen(stream, lambda) // exponential distribution

    override fun nextDelay() = expGen.nextDouble().seconds
    override fun nextObject(): OutputT {
        if (count != null) {
            check(count!! >= 0u)
            count = count!! - 1u
        }
        return obj
    }
    override var empty = false
}