package com.group7.generators

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeBetween
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class DelayProviderTest :
    FunSpec({
        val sampleSize = 10_000

        fun sampleDelays(provider: DelayProvider): List<Duration> = List(sampleSize) { provider.nextDelay() }

        fun List<Duration>.meanSeconds(): Double = map { it.toDouble(DurationUnit.SECONDS) }.average()

        fun List<Duration>.stdDevSeconds(): Double {
            val mean = meanSeconds()
            return kotlin.math.sqrt(map { (it.toDouble(DurationUnit.SECONDS) - mean).let { d -> d * d } }.average())
        }

        context("Delays.normal") {
            test("samples have approximately correct mean and std dev") {
                val mean = 30.seconds
                val stdDev = 5.seconds
                val provider = Delays.normal(mean, stdDev)

                val samples = sampleDelays(provider)

                samples.meanSeconds().shouldBeBetween(28.0, 32.0, 0.0)
                samples.stdDevSeconds().shouldBeBetween(4.0, 6.0, 0.0)
            }

            test("negative samples are clamped to zero") {
                val mean = 1.seconds
                val stdDev = 10.seconds
                val provider = Delays.normal(mean, stdDev)

                val samples = sampleDelays(provider)
                samples.all { it >= Duration.ZERO } shouldBe true
            }

            test("works with explicit DurationUnit") {
                val mean = 2.minutes
                val stdDev = 30.seconds
                val provider = Delays.normal(mean, stdDev, DurationUnit.MINUTES)

                val samples = sampleDelays(provider)

                samples.meanSeconds().shouldBeBetween(110.0, 130.0, 0.0)
            }
        }

        context("Delays.gamma") {
            test("samples have approximately correct mean and std dev") {
                val mean = 30.seconds
                val stdDev = 10.seconds
                val provider = Delays.gamma(mean, stdDev)

                val samples = sampleDelays(provider)

                samples.meanSeconds().shouldBeBetween(28.0, 32.0, 0.0)
                samples.stdDevSeconds().shouldBeBetween(8.0, 12.0, 0.0)
            }

            test("all samples are non-negative") {
                val mean = 5.seconds
                val stdDev = 3.seconds
                val provider = Delays.gamma(mean, stdDev)

                val samples = sampleDelays(provider)
                samples.all { it >= Duration.ZERO } shouldBe true
            }

            test("works with explicit DurationUnit") {
                val mean = 2.minutes
                val stdDev = 30.seconds
                val provider = Delays.gamma(mean, stdDev, DurationUnit.MINUTES)

                val samples = sampleDelays(provider)

                samples.meanSeconds().shouldBeBetween(110.0, 130.0, 0.0)
            }
        }

        context("Delays.exponential") {
            test("samples have approximately correct mean") {
                val mean = 30.seconds
                val provider = Delays.exponentialWithMean(mean)

                val samples = sampleDelays(provider)

                samples.meanSeconds().shouldBeBetween(28.0, 32.0, 0.0)
            }
        }
    })
