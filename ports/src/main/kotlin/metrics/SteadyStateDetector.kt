package com.group7.metrics

import kotlin.time.Instant

fun interface SteadyStateDetector {
    fun isSteady(currentTime: Instant): Boolean
}
