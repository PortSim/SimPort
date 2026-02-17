package com.group7.utils

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
