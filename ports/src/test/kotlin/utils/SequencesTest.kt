package com.group7.utils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class SequencesTest :
    FunSpec({
        context("zipCompletely") {
            test("zips two sequences of equal length") {
                val result = sequenceOf(1, 2, 3).zipCompletely(sequenceOf("a", "b", "c")).toList()
                result shouldBe listOf(1 to "a", 2 to "b", 3 to "c")
            }

            test("zips two empty sequences") {
                val result = emptySequence<Int>().zipCompletely(emptySequence<String>()).toList()
                result shouldBe emptyList()
            }

            test("throws when first sequence has leftover elements") {
                val exception =
                    shouldThrow<IllegalStateException> {
                        sequenceOf(1, 2, 3).zipCompletely(sequenceOf("a", "b")).toList()
                    }
                exception.message shouldContain "Elements were left unzipped"
            }

            test("succeeds when second sequence has leftover elements") {
                val result = sequenceOf(1, 2).zipCompletely(sequenceOf("a", "b", "c")).toList()
                result shouldBe listOf(1 to "a", 2 to "b")
            }

            test("zips single-element sequences") {
                val result = sequenceOf(1).zipCompletely(sequenceOf("a")).toList()
                result shouldBe listOf(1 to "a")
            }
        }
    })
