package com.group7.metrics

import com.group7.metrics.confidence.InstantaneousConfidenceIntervals
import com.group7.metrics.steady.SteadyStateDetector
import com.group7.utils.studentT
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeNaN
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private fun timeAt(i: Int): Instant = Instant.fromEpochMilliseconds(0) + i.minutes

private class TestInstantaneousForCI : InstantaneousMetric() {
    fun fire(time: Instant, value: Double) = notify(time, value)
}

class ConfidenceIntervalsTest :
    FunSpec({
        test("returns NaN before steady state") {
            val metric = TestInstantaneousForCI()
            val neverSteady = SteadyStateDetector { false }
            val ci = InstantaneousConfidenceIntervals(metric, steadyStateDetector = neverSteady)

            // Fire some events
            metric.fire(timeAt(0), 5.0)
            metric.fire(timeAt(1), 10.0)

            ci.mean.report(timeAt(2)).shouldBeNaN()
            ci.lower.report(timeAt(2)).shouldBeNaN()
            ci.upper.report(timeAt(2)).shouldBeNaN()
            ci.variance.report(timeAt(2)).shouldBeNaN()
        }

        test("returns NaN when batch count below target") {
            val metric = TestInstantaneousForCI()
            val alwaysSteady = SteadyStateDetector { true }
            // targetBatches defaults to 32 — with only a few events, won't reach that
            val ci = InstantaneousConfidenceIntervals(metric, steadyStateDetector = alwaysSteady)

            metric.fire(timeAt(0), 5.0)
            metric.fire(timeAt(1), 10.0)
            // Only 2 batches (batch size 1), but target is 32
            ci.mean.report(timeAt(2)).shouldBeNaN()
        }

        test("computes correct CI after enough batches") {
            val metric = TestInstantaneousForCI()
            val alwaysSteady = SteadyStateDetector { true }
            // targetBatches=2 so we only need 2 batches
            val ci = InstantaneousConfidenceIntervals(metric, alpha = 0.05, steadyStateDetector = alwaysSteady)

            // With targetBatches=2 (from AdaptiveSampleBatchMeans default=32), we need 32 samples.
            // Instead, let's feed enough to fill default 32 batches.
            val values = (1..32).map { it.toDouble() }
            for ((i, v) in values.withIndex()) {
                metric.fire(timeAt(i), v)
            }

            val t = timeAt(values.size)
            val meanVal = ci.mean.report(t)
            val varianceVal = ci.variance.report(t)
            val lowerVal = ci.lower.report(t)
            val upperVal = ci.upper.report(t)

            // Batch means: each batch has 1 sample (batch size 1), so batch means = [1,2,...,32]
            val expectedMean = values.average()
            val expectedVariance = values.sumOf { (it - expectedMean) * (it - expectedMean) } / (values.size - 1)
            val tValue = studentT(values.size - 1, 0.05)
            val se = sqrt(expectedVariance / values.size)
            val expectedLower = expectedMean - tValue * se
            val expectedUpper = expectedMean + tValue * se

            meanVal shouldBe (expectedMean plusOrMinus 1e-6)
            varianceVal shouldBe (expectedVariance plusOrMinus 1e-6)
            lowerVal shouldBe (expectedLower plusOrMinus 1e-6)
            upperVal shouldBe (expectedUpper plusOrMinus 1e-6)
        }

        test("caches result for same timestamp") {
            val metric = TestInstantaneousForCI()
            val alwaysSteady = SteadyStateDetector { true }
            val ci = InstantaneousConfidenceIntervals(metric, steadyStateDetector = alwaysSteady)

            for (i in 0..<32) {
                metric.fire(timeAt(i), i.toDouble())
            }

            val t = timeAt(32)
            val first = ci.mean.report(t)
            val second = ci.mean.report(t)
            first shouldBe second
        }

        test("moments returns Moments with correct metrics") {
            val metric = TestInstantaneousForCI()
            val alwaysSteady = SteadyStateDetector { true }
            val ci = InstantaneousConfidenceIntervals(metric, steadyStateDetector = alwaysSteady)

            val moments = ci.moments()
            moments.mean shouldBe ci.mean
            moments.lowerCi shouldBe ci.lower
            moments.upperCi shouldBe ci.upper
            moments.variance shouldBe ci.variance
            moments.sampleCount.shouldNotBeNull()
        }
    })
