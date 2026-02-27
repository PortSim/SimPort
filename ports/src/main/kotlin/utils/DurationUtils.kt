package com.group7.utils

import java.text.DecimalFormat
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal val DurationUnit.suffix
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

internal val Duration.toStringWithBiggestUnit: String
    get() {
        if (isInfinite()) return toString()

        val units =
            listOf(
                DurationUnit.DAYS,
                DurationUnit.HOURS,
                DurationUnit.MINUTES,
                DurationUnit.SECONDS,
                DurationUnit.MILLISECONDS,
                DurationUnit.MICROSECONDS,
                DurationUnit.NANOSECONDS,
            )

        val abs = absoluteValue
        val formatter = DecimalFormat("#.####") // "#.##" removes trailing zeros and the dot if the number is whole
        for (unit in units) {
            // Check if the absolute magnitude is at least 1 of this unit
            if (abs.toDouble(unit) >= 1.0 || unit == DurationUnit.NANOSECONDS) {
                return "${formatter.format(this.toDouble(unit))}${unit.suffix}"
            }
        }

        return toString()
    }
