package com.group7.metrics

import kotlin.time.Instant

/**
 * Separates a list of metric values into two lists: times and values.
 *
 * Useful for preparing metric data for analysis or visualization.
 *
 * @return a pair of (times, values) lists
 */
fun List<MetricValue>.unzip(): Pair<List<Instant>, List<Double>> = asSequence().map { it.time to it.value }.unzip()
