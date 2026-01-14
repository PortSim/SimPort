package com.group7

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import umontreal.ssj.randvar.ExponentialGen
import umontreal.ssj.rng.MRG32k3a

// A generator interface used only by Source nodes
interface Generator<out T> : Iterator<Pair<T, Duration>>

object Generators {
    fun <T> constantDelay(obj: T, delay: Duration): Generator<T> = generateSequence { obj to delay }.asGenerator()

    fun <T> exponentialDelay(obj: T, lambda: Double): Generator<T> {
        val stream = MRG32k3a() // random number stream
        val expGen = ExponentialGen(stream, lambda) // exponential distribution
        return generateSequence { obj to expGen.nextDouble().seconds }.asGenerator()
    }
}

fun <T> Generator<T>.take(n: Int) = asSequence().take(n).asGenerator()

private fun <T> Sequence<Pair<T, Duration>>.asGenerator(): Generator<T> =
    object : Generator<T>, Iterator<Pair<T, Duration>> by iterator() {}
