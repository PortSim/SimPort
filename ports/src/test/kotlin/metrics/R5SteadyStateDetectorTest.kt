package com.group7.metrics

import com.group7.metrics.steady.R5SteadyStateDetector
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private class FixedMean(private val value: Double) : ContinuousMetric() {
    override fun reportImpl(previousTime: Instant, currentTime: Instant) = value
}

private class TestR5SSD(mean: ContinuousMetric, k: Int, period: Int) : R5SteadyStateDetector(mean, k, period) {
    fun feedSample(time: Instant, sample: Double) = reportSample(time, sample)
}

private fun timeAt(i: Int): Instant = Instant.fromEpochMilliseconds(0) + i.minutes

class R5SteadyStateDetectorTest :
    FunSpec({
        test("not steady initially") {
            val detector = TestR5SSD(FixedMean(0.0), k = 2, period = 100)
            detector.isSteady(timeAt(0)) shouldBe false
        }

        test("becomes steady after k crossings") {
            val detector = TestR5SSD(FixedMean(5.0), k = 3, period = 100)
            // Alternating above and below the mean of 5.0 to create crossings
            val samples = listOf(6.0, 4.0, 6.0, 4.0) // 3 crossings: 6->4, 4->6, 6->4
            for ((i, s) in samples.withIndex()) {
                detector.feedSample(timeAt(i), s)
            }
            detector.isSteady(timeAt(samples.size)) shouldBe true
        }

        test("does not become steady with fewer than k crossings") {
            val detector = TestR5SSD(FixedMean(5.0), k = 3, period = 100)
            // Only 2 crossings: 6->4, 4->6
            val samples = listOf(6.0, 4.0, 6.0)
            for ((i, s) in samples.withIndex()) {
                detector.feedSample(timeAt(i), s)
            }
            detector.isSteady(timeAt(samples.size)) shouldBe false
        }

        test("samples on the same side of the mean do not count as crossings") {
            val detector = TestR5SSD(FixedMean(5.0), k = 1, period = 100)
            // All above the mean — no crossing
            val samples = listOf(6.0, 7.0, 8.0, 9.0)
            for ((i, s) in samples.withIndex()) {
                detector.feedSample(timeAt(i), s)
            }
            detector.isSteady(timeAt(samples.size)) shouldBe false
        }

        test("sample equal to mean has sign 0 and does not cross") {
            val detector = TestR5SSD(FixedMean(5.0), k = 1, period = 100)
            // 6.0 -> 5.0: signs are +1 and 0, product is 0 (not < 0), so no crossing
            detector.feedSample(timeAt(0), 6.0)
            detector.feedSample(timeAt(1), 5.0)
            detector.isSteady(timeAt(2)) shouldBe false
        }

        test("stays steady once reached") {
            val detector = TestR5SSD(FixedMean(5.0), k = 1, period = 100)
            detector.feedSample(timeAt(0), 6.0)
            detector.feedSample(timeAt(1), 4.0) // 1 crossing
            detector.isSteady(timeAt(2)) shouldBe true

            // Feed more samples — should remain steady
            detector.feedSample(timeAt(3), 100.0)
            detector.isSteady(timeAt(4)) shouldBe true
        }

        test("new window resets crossings count") {
            // period=3: reset triggers when samples > 3, i.e. on the 4th call
            val detector = TestR5SSD(FixedMean(5.0), k = 3, period = 3)
            // Window 1: samples 0-2 accumulate 2 crossings
            detector.feedSample(timeAt(0), 6.0) // samples=1, previousSign=NaN → +1
            detector.feedSample(timeAt(1), 4.0) // samples=2, crossing 1
            detector.feedSample(timeAt(2), 6.0) // samples=3, crossing 2
            // 4th call triggers reset (samples=4 > 3), crossings reset to 0
            detector.feedSample(timeAt(3), 4.0) // samples→4→reset, previousSign=NaN → -1
            detector.isSteady(timeAt(4)) shouldBe false
        }

        test("previousSign resets on new window - regression for NaN bug") {
            // Before the fix, previousSign was NOT reset when a new window started.
            // This meant the first sample in a new window was compared against the
            // last sample of the previous window, which could produce a spurious crossing.
            //
            // period=2: reset triggers when samples > 2, i.e. on the 3rd call
            val detector = TestR5SSD(FixedMean(5.0), k = 1, period = 2)

            // Window 1: 2 samples, both above the mean — no crossings
            detector.feedSample(timeAt(0), 6.0) // samples=1, previousSign=NaN → +1
            detector.feedSample(timeAt(1), 7.0) // samples=2, same side, no crossing

            // 3rd call triggers reset (samples=3 > 2)
            // First sample of window 2 is below the mean
            detector.feedSample(timeAt(2), 4.0) // reset, previousSign=NaN → -1
            // BUG (old): previousSign stayed at +1, currentSign=-1, product<0 → spurious crossing → steady!
            // FIX (new): previousSign=NaN → just sets -1, no crossing

            detector.isSteady(timeAt(3)) shouldBe false

            // Now create a real crossing within window 2
            detector.feedSample(timeAt(4), 6.0) // crossing: -1 → +1
            detector.isSteady(timeAt(5)) shouldBe true
        }

        test("isSteady calls update") {
            // R5SteadyStateDetector.isSteady calls update() which subclasses override.
            // We verify the base class calls update by checking that isSteady can trigger
            // steady state through the update mechanism.
            var updateCount = 0
            val mean = FixedMean(5.0)
            val detector =
                object : R5SteadyStateDetector(mean, k = 1, period = 100) {
                    override fun update(currentTime: Instant) {
                        updateCount++
                        reportSample(currentTime, if (updateCount == 1) 6.0 else 4.0)
                    }
                }
            detector.isSteady(timeAt(0)) shouldBe false // first call: sets previousSign
            detector.isSteady(timeAt(1)) shouldBe true // second call: crossing
            updateCount shouldBe 2
        }
    })
