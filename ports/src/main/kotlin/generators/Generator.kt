package com.group7.generators

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import umontreal.ssj.randvar.ExponentialGen
import umontreal.ssj.rng.MRG32k3a

// A generator interface used only by Source nodes
interface Generator<out T> : Iterator<Pair<T, Duration>>

fun interface DelayProvider {
    fun nextDelay(): Duration
}

object Generators {
    fun <T> constant(obj: T, delayProvider: DelayProvider): Generator<T> =
        generateSequence { obj to delayProvider.nextDelay() }.asGenerator()

    fun <T> alternating(vararg objs: T, delayProvider: DelayProvider): Generator<T> =
        generateSequence { objs.asList() }.flatten().map { it to delayProvider.nextDelay() }.asGenerator()
}

object Delays {
    fun fixed(delay: Duration) = DelayProvider { delay }

    fun exponential(lambda: Double, unit: DurationUnit): DelayProvider {
        val stream = MRG32k3a() // random number stream
        val expGen = ExponentialGen(stream, lambda) // exponential distribution
        return DelayProvider { expGen.nextDouble().toDuration(unit) }
    }

    fun exponentialWithMean(mean: Duration) = exponential(1 / mean.toDouble(DurationUnit.SECONDS), DurationUnit.SECONDS)
}

fun <T> Generator<T>.take(n: Int) = asSequence().take(n).asGenerator()

internal fun <T> Sequence<Pair<T, Duration>>.asGenerator(): Generator<T> =
    object : Generator<T>, Iterator<Pair<T, Duration>> by iterator() {}
