package com.group7.components

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe

class HistogramChartTest :
    FunSpec({
        context("HistogramData.formatBoundary") {
            test("linear mode formats to 2dp") {
                val data = HistogramData(emptyList(), emptyMap(), logScale = false)
                data.formatBoundary(1.2345) shouldBe "1.23"
            }

            test("log mode formats as log10") {
                val data = HistogramData(emptyList(), emptyMap(), logScale = true)
                data.formatBoundary(100.0) shouldBe "2.00"
            }

            test("log mode with non-positive falls back to direct format") {
                val data = HistogramData(emptyList(), emptyMap(), logScale = true)
                // log10(0) is -Infinity, but the condition is value > 0
                data.formatBoundary(0.0) shouldBe "0.00"
            }
        }

        context("computeNiceMaxAndStepDouble") {
            test("zero or negative max returns 1.0 to 1.0") {
                computeNiceMaxAndStepDouble(0.0) shouldBe (1.0 to 1.0)
                computeNiceMaxAndStepDouble(-5.0) shouldBe (1.0 to 1.0)
            }

            test("nice max is >= actual max") {
                val (niceMax, _) = computeNiceMaxAndStepDouble(47.0)
                niceMax shouldBeGreaterThan 46.9
            }

            test("step divides evenly into nice max") {
                val (niceMax, step) = computeNiceMaxAndStepDouble(47.0)
                val remainder = niceMax % step
                // Allow small floating point error
                remainder shouldBeLessThanOrEqual 1e-10
            }

            test("small values produce small steps") {
                val (niceMax, step) = computeNiceMaxAndStepDouble(0.005)
                step shouldBeLessThanOrEqual 0.005
                niceMax shouldBeGreaterThan 0.0
            }
        }

        context("computeDisplayValues") {
            val bins = listOf(HistogramBin(0.0, 1.0), HistogramBin(1.0, 2.0))
            val freqs = mapOf("A" to listOf(10, 30))
            val data = HistogramData(bins, freqs)

            test("without density returns raw counts as doubles") {
                val result = computeDisplayValues(data, showDensity = false)
                result["A"] shouldBe listOf(10.0, 30.0)
            }

            test("with density returns relative frequencies") {
                val result = computeDisplayValues(data, showDensity = true)
                result["A"] shouldBe listOf(0.25, 0.75)
            }

            test("with density and zero total returns zeros") {
                val zeroData = HistogramData(bins, mapOf("A" to listOf(0, 0)))
                val result = computeDisplayValues(zeroData, showDensity = true)
                result["A"] shouldBe listOf(0.0, 0.0)
            }

            test("empty bins with density returns empty list") {
                val emptyData = HistogramData(emptyList(), mapOf("A" to emptyList()))
                val result = computeDisplayValues(emptyData, showDensity = true)
                result["A"] shouldBe emptyList()
            }

            test("multiple scenarios are independent") {
                val multiData = HistogramData(bins, mapOf("A" to listOf(10, 30), "B" to listOf(20, 20)))
                val result = computeDisplayValues(multiData, showDensity = true)
                result["A"] shouldBe listOf(0.25, 0.75)
                result["B"] shouldBe listOf(0.5, 0.5)
            }
        }

        context("computeHistogram") {
            // computeHistogram requires dynahist Histogram instances.
            // These are integration-level tests using real dynahist histograms.
            test("empty input returns empty data") {
                val result = computeHistogram(emptyMap())
                result.bins shouldHaveSize 0
                result.frequenciesByScenario.shouldBeEmpty()
            }
        }
    })
