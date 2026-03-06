package com.group7.metrics

import com.group7.metrics.steady.R5Continuous
import com.group7.metrics.steady.R5Instantaneous
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private fun timeAt(i: Int): Instant = Instant.fromEpochMilliseconds(0) + i.minutes

/** ContinuousMetric that alternates between two values. */
private class AlternatingMetric(private val low: Double, private val high: Double) : ContinuousMetric() {
    private var isHigh = false

    override fun reportImpl(previousTime: Instant, currentTime: Instant): Double {
        isHigh = !isHigh
        return if (isHigh) high else low
    }
}

/** ContinuousMetric that always returns the same value. */
private class FixedValueMetric(private val value: Double) : ContinuousMetric() {
    override fun reportImpl(previousTime: Instant, currentTime: Instant) = value
}

private class TestInstantaneousForSSD : InstantaneousMetric() {
    fun fire(time: Instant, value: Double) = notify(time, value)
}

class SteadyStateSubclassTest :
    FunSpec({
        context("R5Continuous") {
            test("reports samples from continuous metric and can reach steady state") {
                // Use k=1 and large period so we reach steady quickly
                val metric = AlternatingMetric(3.0, 7.0)
                val detector = R5Continuous(metric, k = 1, period = 100)

                // Need to call isSteady which triggers update → reads from metric
                detector.isSteady(timeAt(0)) shouldBe false
                detector.isSteady(timeAt(1)) shouldBe false
                // After enough alternations and crossings, should become steady
                for (i in 2..10) {
                    detector.isSteady(timeAt(i))
                }
                detector.isSteady(timeAt(11)) shouldBe true
            }

            test("skips duplicate consecutive values") {
                // If the metric returns the same value, no new sample is reported
                val metric = FixedValueMetric(5.0)
                val detector = R5Continuous(metric, k = 1, period = 100)

                // Call isSteady many times — same value means no crossings
                for (i in 0..20) {
                    detector.isSteady(timeAt(i))
                }
                // Should never become steady since there are no crossings
                detector.isSteady(timeAt(21)) shouldBe false
            }
        }

        context("R5Instantaneous") {
            test("hooks into metric onFire and reaches steady state") {
                val metric = TestInstantaneousForSSD()
                val detector = R5Instantaneous(metric, k = 2, period = 100)

                detector.isSteady(timeAt(0)) shouldBe false

                // Fire alternating values above and below the mean (mean tracks via SampleMean)
                // SampleMean starts tracking: first fire=10 → mean=10
                metric.fire(timeAt(0), 10.0)
                // mean=10, sample=10, sign=0 → no crossing
                detector.isSteady(timeAt(1)) shouldBe false

                // After firing 0.0: mean=(10+0)/2=5, sample=0, sign=-1
                metric.fire(timeAt(1), 0.0)
                detector.isSteady(timeAt(2)) shouldBe false

                // Fire 20: mean=(10+0+20)/3=10, sample=20, sign=+1 → crossing
                metric.fire(timeAt(2), 20.0)
                detector.isSteady(timeAt(3)) shouldBe false

                // Fire 0: mean=(10+0+20+0)/4=7.5, sample=0, sign=-1 → crossing #2
                metric.fire(timeAt(3), 0.0)
                detector.isSteady(timeAt(4)) shouldBe true
            }
        }
    })
