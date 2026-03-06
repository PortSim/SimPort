package com.group7.metrics

import com.group7.Simulator
import com.group7.metrics.batchmeans.AdaptiveRateBatchMeans
import com.group7.metrics.batchmeans.AdaptiveSampleBatchMeans
import com.group7.metrics.batchmeans.AdaptiveTimeWeightedBatchMeans
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private fun timeAt(i: Int): Instant = Instant.fromEpochMilliseconds(0) + i.minutes

class AdaptiveBatchMeansTest :
    FunSpec({
        context("AdaptiveBatchMeans (via AdaptiveSampleBatchMeans)") {
            test("mean of single batch") {
                val bm = AdaptiveSampleBatchMeans(targetBatches = 32)
                bm.add(10.0) // batch size = 1, so one sample = one batch
                bm.batchCount() shouldBe 1
                bm.mean() shouldBe 10.0
            }

            test("mean of multiple batches") {
                val bm = AdaptiveSampleBatchMeans(targetBatches = 32)
                bm.add(4.0)
                bm.add(8.0)
                bm.add(6.0)
                bm.batchCount() shouldBe 3
                bm.mean() shouldBe 6.0
            }

            test("batchVariance with two batches") {
                val bm = AdaptiveSampleBatchMeans(targetBatches = 32)
                bm.add(4.0)
                bm.add(8.0)
                // variance = ((4-6)^2 + (8-6)^2) / (2-1) = 8
                bm.batchVariance() shouldBe 8.0
            }

            test("batchVariance requires at least 2 batches") {
                val bm = AdaptiveSampleBatchMeans(targetBatches = 32)
                bm.add(5.0)
                shouldThrow<IllegalArgumentException> { bm.batchVariance() }
            }

            test("collapse halves batch count when reaching 2*target") {
                val bm = AdaptiveSampleBatchMeans(targetBatches = 2)
                // batch size starts at 1, so each add creates one batch
                bm.add(1.0) // batch 1
                bm.add(2.0) // batch 2
                bm.add(3.0) // batch 3
                bm.batchCount() shouldBe 3
                bm.add(4.0) // batch 4 = 2*targetBatches → collapse to 2
                bm.batchCount() shouldBe 2
            }

            test("collapse preserves mean") {
                // Pairwise averaging preserves the overall mean:
                // [2,4,6,8] → [(2+4)/2, (6+8)/2] = [3,7] → mean still 5.0
                val bm = AdaptiveSampleBatchMeans(targetBatches = 2)
                bm.add(2.0)
                bm.add(4.0)
                bm.add(6.0)
                bm.add(8.0) // 4 batches = 2*target → collapse
                // Mean of [2,4,6,8] = 5.0, and collapsed [3,7] also has mean 5.0
                bm.mean() shouldBe (5.0 plusOrMinus 1e-10)
                bm.batchCount() shouldBe 2
            }

            test("onCloseBatch callback fires on each batch close") {
                val bm = AdaptiveSampleBatchMeans(targetBatches = 32)
                var callbackCount = 0
                bm.onCloseBatch { callbackCount++ }
                bm.add(1.0) // closes batch → callback fires
                bm.add(2.0) // closes batch → callback fires
                callbackCount shouldBe 2
            }

            test("multiple onCloseBatch callbacks chain") {
                val bm = AdaptiveSampleBatchMeans(targetBatches = 32)
                var count1 = 0
                var count2 = 0
                bm.onCloseBatch { count1++ }
                bm.onCloseBatch { count2++ }
                bm.add(1.0)
                count1 shouldBe 1
                count2 shouldBe 1
            }
        }

        context("AdaptiveSampleBatchMeans") {
            test("batch size doubles after collapse") {
                val bm = AdaptiveSampleBatchMeans(targetBatches = 2)
                // Initial batch size = 1. Feed 4 to trigger collapse.
                bm.add(1.0) // batch 1
                bm.add(2.0) // batch 2
                bm.add(3.0) // batch 3
                bm.add(4.0) // batch 4 → collapse, batchSize becomes 2
                bm.batchCount() shouldBe 2

                // Now need 2 samples to make a new batch
                bm.add(10.0) // partial, no new batch
                bm.batchCount() shouldBe 2
                bm.add(20.0) // completes batch
                bm.batchCount() shouldBe 3
            }

            test("mean is correct for uniform values") {
                val bm = AdaptiveSampleBatchMeans(targetBatches = 32)
                repeat(10) { bm.add(5.0) }
                bm.mean() shouldBe 5.0
            }

            test("partial batch not included in mean") {
                val bm = AdaptiveSampleBatchMeans(targetBatches = 2)
                // Trigger collapse so batch size becomes 2
                bm.add(1.0)
                bm.add(2.0)
                bm.add(3.0)
                bm.add(4.0) // collapse, batchSize=2
                val countAfterCollapse = bm.batchCount()

                // Add one sample (partial batch — not enough for new batch)
                bm.add(100.0)
                bm.batchCount() shouldBe countAfterCollapse
            }
        }

        context("AdaptiveRateBatchMeans") {
            test("single batch rate is count over interval") {
                val bm =
                    AdaptiveRateBatchMeans(
                        durationUnit = kotlin.time.DurationUnit.MILLISECONDS,
                        initialBatchInterval = 10.milliseconds,
                        targetBatches = 32,
                    )
                val start = Simulator.START_TIME
                // 5 events within the first 10ms batch
                for (i in 0..<5) {
                    bm.update(start + (i * 2).milliseconds)
                }
                // Close the batch by going past 10ms
                bm.update(start + 10.milliseconds)
                bm.batchCount() shouldBe 1
                // rate = 5 events / 10ms = 0.5 per ms
                bm.mean() shouldBe (0.5 plusOrMinus 1e-10)
            }

            test("time jump spans multiple batches") {
                val bm =
                    AdaptiveRateBatchMeans(
                        durationUnit = kotlin.time.DurationUnit.MINUTES,
                        initialBatchInterval = 1.minutes,
                        targetBatches = 32,
                    )
                val start = Simulator.START_TIME
                bm.update(start) // 1 event
                // Jump forward 3 minutes — should close at least 3 batches
                bm.update(start + 3.minutes)
                bm.batchCount() shouldBe 3
            }

            test("batch interval doubles after collapse") {
                val bm =
                    AdaptiveRateBatchMeans(
                        durationUnit = kotlin.time.DurationUnit.MILLISECONDS,
                        initialBatchInterval = 1.milliseconds,
                        targetBatches = 2,
                    )
                val start = Simulator.START_TIME
                // Fill 4 batches (2*targetBatches) to trigger collapse
                // Each batch is 1ms, so jump by 4ms
                bm.update(start + 4.milliseconds)
                bm.batchCount() shouldBe 2 // collapsed from 4 to 2

                // After collapse, interval should be 2ms
                // Jump by 2ms to close the next batch
                bm.update(start + 6.milliseconds)
                bm.batchCount() shouldBe 3
            }

            test("rejects time going backwards") {
                val bm =
                    AdaptiveRateBatchMeans(
                        durationUnit = kotlin.time.DurationUnit.MINUTES,
                        initialBatchInterval = 1.minutes,
                        targetBatches = 32,
                    )
                val start = Simulator.START_TIME
                bm.update(start + 5.minutes)
                shouldThrow<IllegalArgumentException> { bm.update(start + 3.minutes) }
            }
        }

        context("AdaptiveTimeWeightedBatchMeans") {
            test("first update initializes without adding batch") {
                val bm = AdaptiveTimeWeightedBatchMeans(initialBatchInterval = 1.minutes, targetBatches = 32)
                bm.update(timeAt(0), 5.0)
                bm.batchCount() shouldBe 0
            }

            test("constant value produces that value as mean") {
                val bm = AdaptiveTimeWeightedBatchMeans(initialBatchInterval = 1.minutes, targetBatches = 32)
                bm.update(timeAt(0), 7.0)
                bm.update(timeAt(1), 7.0) // closes 1 batch with area=7*1=7, mean=7/1=7
                bm.update(timeAt(2), 7.0) // closes another batch
                bm.batchCount() shouldBe 2
                bm.mean() shouldBe (7.0 plusOrMinus 1e-10)
            }

            test("time-weighted area calculated correctly") {
                val bm = AdaptiveTimeWeightedBatchMeans(initialBatchInterval = 2.minutes, targetBatches = 32)
                // value=2 from t=0 to t=1, then value=6 from t=1 to t=2
                bm.update(timeAt(0), 2.0)
                bm.update(timeAt(1), 6.0)
                bm.update(timeAt(2), 0.0) // closes batch: area = 2*1 + 6*1 = 8, mean = 8/2 = 4
                bm.batchCount() shouldBe 1
                bm.mean() shouldBe (4.0 plusOrMinus 1e-10)
            }

            test("batch interval doubles after collapse") {
                val bm = AdaptiveTimeWeightedBatchMeans(initialBatchInterval = 1.minutes, targetBatches = 2)
                bm.update(timeAt(0), 5.0)
                // 4 minutes pass → 4 batches → collapse to 2, interval doubles to 2min
                bm.update(timeAt(4), 5.0)
                bm.batchCount() shouldBe 2

                // Next batch should need 2 minutes
                bm.update(timeAt(6), 5.0) // closes 1 more batch
                bm.batchCount() shouldBe 3
            }

            test("rejects time going backwards") {
                val bm = AdaptiveTimeWeightedBatchMeans(initialBatchInterval = 1.minutes, targetBatches = 32)
                bm.update(timeAt(5), 1.0)
                shouldThrow<IllegalArgumentException> { bm.update(timeAt(3), 2.0) }
            }
        }
    })
