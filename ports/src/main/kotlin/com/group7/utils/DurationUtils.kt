package com.group7.utils

import java.text.DecimalFormat
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.DurationUnit

val DurationUnit.suffix
    get() =
        when (this) {
            DurationUnit.NANOSECONDS -> "ns"
            DurationUnit.MICROSECONDS -> "µs"
            DurationUnit.MILLISECONDS -> "ms"
            DurationUnit.SECONDS -> "s"
            DurationUnit.MINUTES -> "min"
            DurationUnit.HOURS -> "h"
            DurationUnit.DAYS -> "d"
        }

private val durationUnits =
    listOf(
        DurationUnit.DAYS,
        DurationUnit.HOURS,
        DurationUnit.MINUTES,
        DurationUnit.SECONDS,
        DurationUnit.MILLISECONDS,
        DurationUnit.MICROSECONDS,
        DurationUnit.NANOSECONDS,
    )

fun Duration.toStringWithBestUnit(dp: Int = 4): String {
    val formatter = DecimalFormat("#." + "#".repeat(dp))
    val unit =
        durationUnits.firstOrNull() {
            // Check if the absolute magnitude is at least 1 of this unit
            absoluteValue.toDouble(it) >= 1.0
        } ?: durationUnits.last()
    return "${formatter.format(this.toDouble(unit))}${unit.suffix}"
}

fun Number.toRateStringWithBestUnit(duration: Duration, dp: Int = 4): String {
    if (duration == Duration.ZERO) {
        return "0/d"
    }

    val formatter = DecimalFormat("#." + "#".repeat(dp))
    val unit =
        durationUnits.lastOrNull() {
            // Find the smallest unit such that (value / duration) is >= 1,
            // as the unit of duration gets larger, this value gets larger
            // so we use the last one from the list
            val value = this.toDouble() / duration.toDouble(it)
            value.absoluteValue >= 1.0
        } ?: durationUnits.first()
    val value = this.toDouble() / duration.toDouble(unit)
    return "${formatter.format(value)}/${unit.suffix}"
}
