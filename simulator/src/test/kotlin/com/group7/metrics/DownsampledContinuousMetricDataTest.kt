package com.group7.metrics

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSortedWith
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.time.Instant

private fun timeAt(i: Int): Instant = Instant.fromEpochMilliseconds(i.toLong())

class DownsampledContinuousMetricDataTest :
    FunSpec({
        test("empty state has no values") {
            val data = DownsampledContinuousMetricData()
            data.values.shouldBeEmpty()
        }

        test("single sample is stored") {
            val data = DownsampledContinuousMetricData()
            data.add(timeAt(0), 5.0)
            data.values.size shouldBeExactly 1
            data.values[0].time shouldBe timeAt(0)
            data.values[0].value shouldBe 5.0
        }

        test("consecutive identical values are compressed") {
            val data = DownsampledContinuousMetricData()
            data.add(timeAt(0), 10.0)
            data.add(timeAt(1), 10.0)
            data.add(timeAt(2), 10.0)
            data.add(timeAt(3), 10.0)
            // Only the first sample should be stored; the rest are skipped
            data.values.size shouldBeExactly 1
        }

        test("value change after skipped duplicates inserts boundary sample") {
            val data = DownsampledContinuousMetricData()
            data.add(timeAt(0), 10.0)
            data.add(timeAt(1), 10.0) // skipped
            data.add(timeAt(2), 10.0) // skipped
            data.add(timeAt(3), 20.0) // value changes -> boundary at t=2 + new at t=3
            data.values.size shouldBeExactly 3
            // First: original
            data.values[0].time shouldBe timeAt(0)
            data.values[0].value shouldBe 10.0
            // Second: boundary closing the previous constant region at last skipped time
            data.values[1].time shouldBe timeAt(2)
            data.values[1].value shouldBe 10.0
            // Third: the new value
            data.values[2].time shouldBe timeAt(3)
            data.values[2].value shouldBe 20.0
        }

        test("alternating values are all stored") {
            val data = DownsampledContinuousMetricData()
            data.add(timeAt(0), 1.0)
            data.add(timeAt(1), 2.0)
            data.add(timeAt(2), 1.0)
            data.add(timeAt(3), 2.0)
            data.values.size shouldBeExactly 4
        }

        test("boundary sample is not inserted when there are no skipped samples") {
            val data = DownsampledContinuousMetricData()
            data.add(timeAt(0), 1.0)
            data.add(timeAt(1), 2.0) // different value, no skipped samples before
            data.values.size shouldBeExactly 2
        }

        test("multiple constant regions produce correct boundaries") {
            val data = DownsampledContinuousMetricData()
            // Region 1: value=5 from t=0 to t=3
            data.add(timeAt(0), 5.0)
            data.add(timeAt(1), 5.0) // skipped
            data.add(timeAt(2), 5.0) // skipped
            data.add(timeAt(3), 5.0) // skipped
            // Region 2: value=10 from t=4 to t=7
            data.add(timeAt(4), 10.0) // boundary at t=3 (5.0) + new at t=4 (10.0)
            data.add(timeAt(5), 10.0) // skipped
            data.add(timeAt(6), 10.0) // skipped
            data.add(timeAt(7), 10.0) // skipped
            // Region 3: value=15 at t=8
            data.add(timeAt(8), 15.0) // boundary at t=7 (10.0) + new at t=8 (15.0)

            data.values.size shouldBeExactly 5
            data.values.map { it.value } shouldBe listOf(5.0, 5.0, 10.0, 10.0, 15.0)
            data.values.map { it.time } shouldBe listOf(timeAt(0), timeAt(3), timeAt(4), timeAt(7), timeAt(8))
        }

        test("downsampling triggers at 2x DESIRED_SAMPLES and reduces size") {
            val data = DownsampledContinuousMetricData()
            val trigger = 2 * DownsampledContinuousMetricData.DESIRED_SAMPLES
            // Add enough distinct values to trigger downsampling
            for (i in 0 until trigger) {
                data.add(timeAt(i), i.toDouble())
            }
            data.values.size shouldBeLessThanOrEqual DownsampledContinuousMetricData.DESIRED_SAMPLES
            data.values.size shouldBeGreaterThan 0
        }

        test("first and last points are preserved after downsampling") {
            val data = DownsampledContinuousMetricData()
            val trigger = 2 * DownsampledContinuousMetricData.DESIRED_SAMPLES
            for (i in 0 until trigger) {
                data.add(timeAt(i), i.toDouble())
            }
            data.values.first().time shouldBe timeAt(0)
            data.values.first().value shouldBe 0.0
            data.values.last().time shouldBe timeAt(trigger - 1)
            data.values.last().value shouldBe (trigger - 1).toDouble()
        }

        test("values remain sorted by time after downsampling") {
            val data = DownsampledContinuousMetricData()
            val totalSamples = DownsampledContinuousMetricData.DESIRED_SAMPLES * 2 + 2000
            for (i in 0 until totalSamples) {
                data.add(timeAt(i), i.toDouble())
            }
            data.values.shouldBeSortedWith(compareBy { it.time })
        }

        test("downsampling with sparse data preserves shape") {
            val data = DownsampledContinuousMetricData()
            val totalSamples = 2 * DownsampledContinuousMetricData.DESIRED_SAMPLES
            val spikeMid = totalSamples / 2
            // Create a signal with a big spike in the middle
            for (i in 0 until totalSamples) {
                val value = if (i in (spikeMid - 10)..(spikeMid + 10)) 1000.0 else 0.0
                data.add(timeAt(i), value)
            }
            // The spike region should be represented in the downsampled data
            val spikeValues = data.values.filter { it.value == 1000.0 }
            spikeValues.size shouldBeGreaterThan 0
        }

        test("zero-duration data does not crash") {
            val data = DownsampledContinuousMetricData()
            val sameTime = timeAt(0)
            val totalSamples = 2 * DownsampledContinuousMetricData.DESIRED_SAMPLES
            // All samples at the same instant with different values
            for (i in 0 until totalSamples) {
                data.add(sameTime, i.toDouble())
            }
            // Should not crash; downsampling bails out on zero duration
            data.values.size shouldBeGreaterThan 0
        }

        test("adding samples after downsampling continues to work") {
            val data = DownsampledContinuousMetricData()
            val trigger = 2 * DownsampledContinuousMetricData.DESIRED_SAMPLES
            // Trigger first downsampling
            for (i in 0 until trigger) {
                data.add(timeAt(i), i.toDouble())
            }
            val sizeAfterFirst = data.values.size
            sizeAfterFirst shouldBeLessThanOrEqual DownsampledContinuousMetricData.DESIRED_SAMPLES

            // Add more samples
            val extra = 2000
            for (i in trigger until trigger + extra) {
                data.add(timeAt(i), i.toDouble())
            }
            data.values.size shouldBeGreaterThan 0
            data.values.last().value shouldBe (trigger + extra - 1).toDouble()
        }
    })
