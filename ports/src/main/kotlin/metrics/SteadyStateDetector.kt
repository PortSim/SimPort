package com.group7.metrics

import kotlin.time.Instant

/** Steady state detectors check if something is in a steady state so we can start calculating confidence intervals. */
fun interface SteadyStateDetector {
    /** Takes a time and returns `true` if the metric is steady. */
    fun isSteady(currentTime: Instant): Boolean
}
