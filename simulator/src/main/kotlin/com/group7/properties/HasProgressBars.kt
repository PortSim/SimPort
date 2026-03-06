package com.group7.properties

import com.group7.Simulator
import kotlin.time.Duration

interface HasProgressBars {
    fun onCreateProgressBar(
        callback:
            context(Simulator)
            (label: String, delay: Duration) -> Unit
    )
}
