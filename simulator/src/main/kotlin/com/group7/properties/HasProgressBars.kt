package com.group7.properties

import com.group7.Simulator
import kotlin.time.Duration

/** Indicates a node supports showing a progress bar to indicate progress */
interface HasProgressBars {
    /** Callback to execute when the node creates a progress bar */
    fun onCreateProgressBar(
        callback:
            context(Simulator)
            (label: String, delay: Duration) -> Unit
    )
}
