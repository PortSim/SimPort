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

fun interface DelayProvider {
    fun nextDelay(): Duration

    val displayProperty: DisplayProperty
        get() = GroupDisplayProperty("No delay provider display properties provided")
}

fun DelayProvider.withDisplay(property: DisplayProperty): DelayProvider {
    val delegate = this
    return object : DelayProvider {
        override fun nextDelay() = delegate.nextDelay()

        override val displayProperty = property
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
    fun fixed(delay: Duration) =
        DelayProvider { delay }
            .withDisplay(
                GroupDisplayProperty(
                    "Fixed Delay Provider Parameters",
                    DoubleDisplayProperty("delay", delay.toDouble(DurationUnit.SECONDS), DurationUnit.SECONDS.suffix),
                )
            )

    fun exponential(lambda: Double, unit: DurationUnit): DelayProvider {
        val stream = MRG32k3a() // random number stream
        val expGen = ExponentialGen(stream, lambda) // exponential distribution
        val displayProperty =
            GroupDisplayProperty("Exponential Delay Provider", DoubleDisplayProperty("lambda", lambda, unit.suffix))
        return DelayProvider { expGen.nextDouble().toDuration(unit) }.withDisplay(displayProperty)
    }

    fun exponentialWithMean(mean: Duration) =
        exponential(1 / mean.toDouble(DurationUnit.SECONDS), DurationUnit.SECONDS)
            .withDisplay(
                GroupDisplayProperty(
                    "Exponential Delay Provider",
                    DoubleDisplayProperty("mean", mean.toDouble(DurationUnit.SECONDS), DurationUnit.SECONDS.suffix),
                )
            )
}

fun <T> Generator<T>.take(n: Int) = asSequence().take(n).asGenerator()

internal fun <T> Sequence<Pair<T, Duration>>.asGenerator(
    displayProperty: DisplayProperty = GroupDisplayProperty("No generator display properties provided")
): Generator<T> =
    object : Generator<T>, Iterator<Pair<T, Duration>> by iterator() {
        override val displayProperty: DisplayProperty
            get() = displayProperty
    }
