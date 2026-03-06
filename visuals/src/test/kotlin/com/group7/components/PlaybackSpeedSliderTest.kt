package com.group7.components

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PlaybackSpeedSliderTest :
    FunSpec({
        context("formatSpeed") {
            test("large speeds show no decimals") {
                formatSpeed(10f) shouldBe "10x"
                formatSpeed(50f) shouldBe "50x"
                formatSpeed(100f) shouldBe "100x"
            }

            test("medium speeds show one decimal") {
                formatSpeed(1f) shouldBe "1.0x"
                formatSpeed(1.5f) shouldBe "1.5x"
                formatSpeed(9.9f) shouldBe "9.9x"
            }

            test("small speeds show two decimals") {
                formatSpeed(0.5f) shouldBe "0.50x"
                formatSpeed(0.01f) shouldBe "0.01x"
                formatSpeed(0.99f) shouldBe "0.99x"
            }

            test("boundary at 10 rounds to int") { formatSpeed(10.4f) shouldBe "10x" }

            test("boundary just below 1") { formatSpeed(0.999f) shouldBe "1.00x" }
        }
    })
