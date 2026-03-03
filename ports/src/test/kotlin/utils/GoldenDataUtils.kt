package com.group7.utils

import com.group7.EventLog
import com.group7.Scenario
import com.group7.Simulator
import com.group7.metrics.MetricGroup
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.datatest.withTests
import java.nio.file.Path
import kotlin.io.path.*
import org.opentest4j.AssertionFailedError
import org.opentest4j.FileInfo

private val inCI = System.getenv("CI").toBoolean()
private val RESULTS_DIR = Path("src/test/resources/golden/")

/** Compares against stored golden data */
context(scope: FunSpecContainerScope)
suspend fun compareGoldenData(scenario: Scenario, testName: String) {
    val baseDir = RESULTS_DIR / testName
    val comparedFiles = mutableSetOf<Path>()
    val allResults = generateGoldenData(scenario)
    var missingFilesCreated = false

    scope.withTests<Map.Entry<MetricGroup, String>>(
        { (metric) -> "${metric.name} (${metric.associatedNode?.label ?: "<global>"})" },
        allResults.entries,
    ) { (metric, results) ->
        val resultFile = baseDir / metric.name / ((metric.associatedNode?.label ?: "!global") + ".txt")
        comparedFiles.add(resultFile)
        if (!resultFile.exists()) {
            if (inCI) {
                error("Results file $resultFile is missing!")
            }
            resultFile.parent.createDirectories()
            resultFile.writeText(results)
            missingFilesCreated = true
            return@withTests
        }
        val goldenResults = resultFile.readText()
        if (goldenResults.standardiseString() != results.standardiseString()) {
            throw AssertionFailedError(
                "Test results differ from contents of $resultFile",
                FileInfo(resultFile.absolutePathString(), goldenResults.toByteArray()),
                results,
            )
        }
    }

    if (missingFilesCreated) {
        error("Missing files created. Please run the test again.")
    }

    for (file in baseDir.walk()) {
        if (file.isDirectory()) {
            continue
        }
        if (file !in comparedFiles) {
            error("Lingering test file: $file")
        }
    }
}

/** Generates golden data using the scenario provided, assuming only one metric is active at time */
private fun generateGoldenData(scenario: Scenario): Map<MetricGroup, String> {
    val reporter = WriteOutMetricsReporter(scenario)
    // Run simulation
    val simulator = Simulator(EventLog.noop(), scenario, metricReporter = reporter)
    while (!simulator.isFinished) {
        simulator.nextStep()
    }
    return reporter.results()
}

/** Clean up EOL differences between OSs, and standardises the string to be comparable */
private fun String.standardiseString(): String {
    // Trim nonprintable characters, line separator, and trailing whitespace
    return this.trim { it <= ' ' }.convertLineSeparators().trimTrailingWhitespacesAndAddNewlineAtEOF()
}

private fun String.convertLineSeparators(separator: String = "\n"): String {
    return replace(Regex("\r\n|\r|\n"), separator)
}

private fun String.trimTrailingWhitespacesAndAddNewlineAtEOF(): String =
    this.trimTrailingWhitespaces().let { result -> if (result.endsWith("\n")) result else result + "\n" }

private fun String.trimTrailingWhitespaces(): String = this.split('\n').joinToString(separator = "\n") { it.trimEnd() }
