package com.group7.metrics

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.Instant

private fun timeAt(i: Int): Instant = Instant.fromEpochMilliseconds(0) + i.minutes

private class TrackingContinuousMetric(private val value: Double = 1.0) : ContinuousMetric() {
    var reportImplCalls = 0
    val previousTimes = mutableListOf<Instant>()
    val currentTimes = mutableListOf<Instant>()

    override fun reportImpl(previousTime: Instant, currentTime: Instant): Double {
        reportImplCalls++
        previousTimes.add(previousTime)
        currentTimes.add(currentTime)
        return value
    }
}

private class TestInstantaneousMetric : InstantaneousMetric() {
    fun fireEvent(time: Instant, value: Double) = notify(time, value)
}

private class TestRateMetric : RateMetric(DurationUnit.MINUTES) {
    fun fireEvent(time: Instant) = notify(time)
}

class MetricTest :
    FunSpec({
        context("ContinuousMetric") {
            test("first report calls reportImpl with same time for both args") {
                val metric = TrackingContinuousMetric()
                metric.report(timeAt(5))
                metric.previousTimes.single() shouldBe timeAt(5)
                metric.currentTimes.single() shouldBe timeAt(5)
            }

            test("caches value when called with same time") {
                val metric = TrackingContinuousMetric(42.0)
                val first = metric.report(timeAt(1))
                val second = metric.report(timeAt(1))
                first shouldBe 42.0
                second shouldBe 42.0
                metric.reportImplCalls shouldBe 1
            }

            test("rejects time going backwards") {
                val metric = TrackingContinuousMetric()
                metric.report(timeAt(5))
                shouldThrow<IllegalArgumentException> { metric.report(timeAt(3)) }
            }

            test("increments sampleCount on each new time") {
                val metric = TrackingContinuousMetric()
                metric.sampleCount shouldBe 0
                metric.report(timeAt(0))
                metric.sampleCount shouldBe 1
                metric.report(timeAt(1))
                metric.sampleCount shouldBe 2
                // Same time should not increment
                metric.report(timeAt(1))
                metric.sampleCount shouldBe 2
            }
        }

        context("InstantaneousMetric") {
            test("notify fires all listeners") {
                val metric = TestInstantaneousMetric()
                var count = 0
                metric.onFire { _, _ -> count++ }
                metric.onFire { _, _ -> count++ }
                metric.fireEvent(timeAt(0), 1.0)
                count shouldBe 2
            }

            test("listeners receive correct time and value") {
                val metric = TestInstantaneousMetric()
                var receivedTime: Instant? = null
                var receivedValue: Double? = null
                metric.onFire { t, v ->
                    receivedTime = t
                    receivedValue = v
                }
                metric.fireEvent(timeAt(3), 7.5)
                receivedTime shouldBe timeAt(3)
                receivedValue shouldBe 7.5
            }

            test("increments sampleCount on notify") {
                val metric = TestInstantaneousMetric()
                metric.sampleCount shouldBe 0
                metric.fireEvent(timeAt(0), 1.0)
                metric.sampleCount shouldBe 1
                metric.fireEvent(timeAt(1), 2.0)
                metric.sampleCount shouldBe 2
            }
        }

        context("RateMetric") {
            test("notify fires all listeners with time") {
                val metric = TestRateMetric()
                val receivedTimes = mutableListOf<Instant>()
                metric.onFire { t -> receivedTimes.add(t) }
                metric.onFire { t -> receivedTimes.add(t) }
                metric.fireEvent(timeAt(5))
                receivedTimes shouldBe listOf(timeAt(5), timeAt(5))
            }

            test("increments sampleCount on notify") {
                val metric = TestRateMetric()
                metric.sampleCount shouldBe 0
                metric.fireEvent(timeAt(0))
                metric.sampleCount shouldBe 1
                metric.fireEvent(timeAt(1))
                metric.sampleCount shouldBe 2
            }
        }
    })
