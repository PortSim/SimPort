package com.group7.generators

import com.group7.DisplayProperty
import com.group7.DoubleDisplayProperty
import com.group7.GroupDisplayProperty
import com.group7.utils.suffix
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import umontreal.ssj.randvar.ExponentialGen
import umontreal.ssj.rng.MRG32k3a

// A generator interface used only by Source nodes
interface Generator<out T> : Iterator<Pair<T, Duration>> {
    val displayProperty: DisplayProperty
}

interface DelayProvider {
    fun nextDelay(): Duration

    val displayProperty: DisplayProperty

    companion object {
        // This is the ONLY way to create an instance
        operator fun invoke(displayProperty: DisplayProperty, block: () -> Duration): DelayProvider =
            object : DelayProvider {
                override val displayProperty: DisplayProperty
                    get() = displayProperty

                override fun nextDelay(): Duration = block()
            }
    }
}

object Generators {
    fun <T> constant(factory: () -> T, delayProvider: DelayProvider): Generator<T> =
        generateSequence { factory() to delayProvider.nextDelay() }.asGenerator(delayProvider.displayProperty)

    fun <T> alternating(vararg objs: () -> T, delayProvider: DelayProvider): Generator<T> =
        generateSequence { objs.map { it() } }
            .flatten()
            .map { it to delayProvider.nextDelay() }
            .asGenerator(delayProvider.displayProperty)
}

object Delays {
    fun fixed(delay: Duration): DelayProvider {
        val displayProperty =
            GroupDisplayProperty(
                "Fixed Delay Provider Parameters",
                DoubleDisplayProperty("delay", delay.toDouble(DurationUnit.SECONDS), DurationUnit.SECONDS.suffix),
            )
        return DelayProvider(displayProperty) { delay }
    }

    fun exponential(lambda: Double, unit: DurationUnit): DelayProvider {
        val stream = MRG32k3a() // random number stream
        val expGen = ExponentialGen(stream, lambda) // exponential distribution
        val displayProperty =
            GroupDisplayProperty("Exponential Delay Provider", DoubleDisplayProperty("lambda", lambda, unit.suffix))
        return DelayProvider(displayProperty) { expGen.nextDouble().toDuration(unit) }
    }

    fun exponentialWithMean(mean: Duration): DelayProvider {
        val displayProperty =
            GroupDisplayProperty(
                "Exponential Delay Provider",
                DoubleDisplayProperty("mean", mean.toDouble(DurationUnit.SECONDS), DurationUnit.SECONDS.suffix),
            )
        val exp = exponential(1 / mean.toDouble(DurationUnit.SECONDS), DurationUnit.SECONDS)
        return DelayProvider(displayProperty, { exp.nextDelay() })
    }
}

fun <T> Generator<T>.take(n: Int) = asSequence().take(n).asGenerator()

internal fun <T> Sequence<Pair<T, Duration>>.asGenerator(
    displayProperty: DisplayProperty = GroupDisplayProperty("No generator display properties provided")
): Generator<T> =
    object : Generator<T>, Iterator<Pair<T, Duration>> by iterator() {
        override val displayProperty: DisplayProperty
            get() = displayProperty
    }
