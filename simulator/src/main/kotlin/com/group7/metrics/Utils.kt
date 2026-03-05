package com.group7.metrics

import kotlin.time.Instant

fun List<MetricValue>.unzip(): Pair<List<Instant>, List<Double>> = asSequence().map { it.time to it.value }.unzip()
