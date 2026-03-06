package com.group7.metrics

import com.group7.metrics.mean.ContinuousMean
import com.group7.metrics.mean.SampleMean
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeNaN
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private fun timeAt(i: Int): Instant = Instant.fromEpochMilliseconds(0) + i.minutes

private class ConstantMetric(private val value: Double) : ContinuousMetric() {
    override fun reportImpl(previousTime: Instant, currentTime: Instant) = value
}

private class SequenceMetric(private val values: Iterator<Double>) : ContinuousMetric() {
    override fun reportImpl(previousTime: Instant, currentTime: Instant) = values.next()
}

private class TestInstantaneous : InstantaneousMetric() {
    fun fire(time: Instant, value: Double) = notify(time, value)
}

class MeanTest :
    FunSpec({
        context("ContinuousMean") {
            test("reports time-weighted average for constant metric") {
                val raw = ConstantMetric(10.0)
                val mean = ContinuousMean(raw)
                // First report initializes
                mean.report(timeAt(0))
                // Second report should give the constant value
                val result = mean.report(timeAt(5))
                result shouldBe 10.0
            }

            test("weighs values by duration") {
                // Returns 2.0 on first call, then 8.0
                val raw = SequenceMetric(listOf(2.0, 2.0, 8.0).iterator())
                val mean = ContinuousMean(raw)

                // t=0: initializes mean, raw reports 2.0
                mean.report(timeAt(0))
                // t=1: elapsed=1min, raw reports 2.0, area=2*1=2, total=1 → mean=2
                mean.report(timeAt(1))
                // t=3: elapsed=2min, raw reports 8.0, area=2+8*2=18, total=3 → mean=6
                val result = mean.report(timeAt(3))
                result shouldBe 6.0
            }
        }

        context("SampleMean") {
            test("returns NaN when no samples") {
                val raw = TestInstantaneous()
                val mean = SampleMean(raw)
                mean.report(timeAt(0)).shouldBeNaN()
            }

            test("returns correct mean after samples") {
                val raw = TestInstantaneous()
                val mean = SampleMean(raw)
                raw.fire(timeAt(0), 4.0)
                raw.fire(timeAt(1), 6.0)
                // mean = (4+6)/2 = 5
                mean.report(timeAt(2)) shouldBe 5.0
            }

            test("accumulates across multiple fires") {
                val raw = TestInstantaneous()
                val mean = SampleMean(raw)
                raw.fire(timeAt(0), 10.0)
                mean.report(timeAt(1)) shouldBe 10.0
                raw.fire(timeAt(2), 20.0)
                // mean = (10+20)/2 = 15
                mean.report(timeAt(3)) shouldBe 15.0
            }
        }
    })
