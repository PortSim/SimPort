package com.group7.components

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith

class CsvExportTest :
    FunSpec({
        context("csvEscape") {
            test("plain value is unchanged") { csvEscape("hello") shouldBe "hello" }

            test("value with comma is quoted") { csvEscape("a,b") shouldBe "\"a,b\"" }

            test("value with quotes is escaped and quoted") { csvEscape("say \"hi\"") shouldBe "\"say \"\"hi\"\"\"" }

            test("value with newline is quoted") { csvEscape("line1\nline2") shouldBe "\"line1\nline2\"" }

            test("empty string is unchanged") { csvEscape("") shouldBe "" }

            test("value with all special characters") { csvEscape("a,\"b\"\nc") shouldBe "\"a,\"\"b\"\"\nc\"" }
        }

        context("sectionToCsv") {
            test("produces header row followed by data rows") {
                val section =
                    TableSection(
                        title = "Test",
                        columnHeaders = listOf("Name", "Value"),
                        rows =
                            listOf(
                                TableRow(listOf(TableCell("alpha"), TableCell("1.0"))),
                                TableRow(listOf(TableCell("beta"), TableCell("2.0"))),
                            ),
                        fixedColumnCount = 1,
                    )
                val csv = sectionToCsv(section)
                val lines = csv.trimEnd().lines()

                lines.size shouldBe 3
                lines[0] shouldBe "Name,Value"
                lines[1] shouldBe "alpha,1.0"
                lines[2] shouldBe "beta,2.0"
            }

            test("escapes special characters in cells") {
                val section =
                    TableSection(
                        title = "T",
                        columnHeaders = listOf("Col"),
                        rows = listOf(TableRow(listOf(TableCell("has,comma")))),
                        fixedColumnCount = 1,
                    )
                sectionToCsv(section) shouldContain "\"has,comma\""
            }
        }

        context("allSectionsToCsv") {
            test("empty sections returns empty string") {
                allSectionsToCsv(emptyList(), ResultsGrouping.SIMULATION) shouldBe ""
            }

            test("prepends grouping column to each row") {
                val sections =
                    listOf(
                        TableSection(
                            title = "Sim1",
                            columnHeaders = listOf("Metric", "Value"),
                            rows = listOf(TableRow(listOf(TableCell("throughput"), TableCell("42.0")))),
                            fixedColumnCount = 1,
                        ),
                        TableSection(
                            title = "Sim2",
                            columnHeaders = listOf("Metric", "Value"),
                            rows = listOf(TableRow(listOf(TableCell("throughput"), TableCell("43.0")))),
                            fixedColumnCount = 1,
                        ),
                    )
                val csv = allSectionsToCsv(sections, ResultsGrouping.SIMULATION)
                val lines = csv.trimEnd().lines()

                lines.size shouldBe 3
                lines[0] shouldStartWith "Simulation,"
                lines[0] shouldBe "Simulation,Metric,Value"
                lines[1] shouldBe "Sim1,throughput,42.0"
                lines[2] shouldBe "Sim2,throughput,43.0"
            }

            test("uses metric grouping column name") {
                val sections =
                    listOf(
                        TableSection(
                            title = "Throughput",
                            columnHeaders = listOf("Sim", "Value"),
                            rows = listOf(TableRow(listOf(TableCell("A"), TableCell("1")))),
                            fixedColumnCount = 1,
                        )
                    )
                val csv = allSectionsToCsv(sections, ResultsGrouping.METRIC)
                csv.lines().first() shouldStartWith "Metric,"
            }
        }
    })
