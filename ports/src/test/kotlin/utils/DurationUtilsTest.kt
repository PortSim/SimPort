package com.group7.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class DurationUtilsTest :
    FunSpec({
        context("DurationUnit.suffix") {
            test("NANOSECONDS suffix is ns") { DurationUnit.NANOSECONDS.suffix shouldBe "ns" }
            test("MICROSECONDS suffix is µs") { DurationUnit.MICROSECONDS.suffix shouldBe "µs" }
            test("MILLISECONDS suffix is ms") { DurationUnit.MILLISECONDS.suffix shouldBe "ms" }
            test("SECONDS suffix is s") { DurationUnit.SECONDS.suffix shouldBe "s" }
            test("MINUTES suffix is min") { DurationUnit.MINUTES.suffix shouldBe "min" }
            test("HOURS suffix is h") { DurationUnit.HOURS.suffix shouldBe "h" }
            test("DAYS suffix is d") { DurationUnit.DAYS.suffix shouldBe "d" }
        }

        context("Duration.toStringWithBestUnit") {
            test("formats days for large durations") { 2.5.days.toStringWithBestUnit() shouldBe "2.5d" }

            test("formats hours") { 3.5.hours.toStringWithBestUnit() shouldBe "3.5h" }

            test("formats minutes") { 90.seconds.toStringWithBestUnit() shouldBe "1.5min" }

            test("formats seconds") { 1500.milliseconds.toStringWithBestUnit() shouldBe "1.5s" }

            test("formats milliseconds") { 1500.microseconds.toStringWithBestUnit() shouldBe "1.5ms" }

            test("formats microseconds") { 1500.nanoseconds.toStringWithBestUnit() shouldBe "1.5µs" }

            test("formats nanoseconds for very small durations") {
                50.nanoseconds.toStringWithBestUnit() shouldBe "50ns"
            }

            test("formats exactly 1 of a unit") { 1.seconds.toStringWithBestUnit() shouldBe "1s" }

            test("respects dp parameter") {
                val duration = 1.2345.seconds
                duration.toStringWithBestUnit(dp = 2) shouldBe "1.23s"
            }

            test("handles negative durations") { (-2.5).hours.toStringWithBestUnit() shouldBe "-2.5h" }

            test("handles Duration.ZERO") { Duration.ZERO.toStringWithBestUnit() shouldBe "0ns" }
        }

        context("Number.toRateStringWithBestUnit") {
            test("returns 0/d when duration is Duration.ZERO") {
                5.toRateStringWithBestUnit(Duration.ZERO) shouldBe "0/d"
            }

            test("formats rate per second") { 10.toRateStringWithBestUnit(1.seconds) shouldBe "10/s" }

            test("formats rate per day for large durations") { 1.toRateStringWithBestUnit(1.days) shouldBe "1/d" }

            test("selects smallest unit where rate >= 1") {
                // 60 arrivals in 1 hour = 1/min
                60.toRateStringWithBestUnit(1.hours) shouldBe "1/min"
            }

            test("handles fractional rates") {
                // 1 arrival in 2 seconds = 0.5/s, but that's < 1, so it picks /min => 30/min
                // Actually: 1/2s = 0.5/s (< 1), 1/2s in minutes = 30/min (>= 1) => "30/min"
                1.toRateStringWithBestUnit(2.seconds) shouldBe "30/min"
            }

            test("handles negative numbers") { (-10).toRateStringWithBestUnit(1.seconds) shouldBe "-10/s" }
        }
    })
