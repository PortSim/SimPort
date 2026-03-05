package com.group7.properties

import com.group7.Simulator
import kotlin.time.Duration
import kotlin.time.Instant

interface Source<out T> {
    fun onEmit(
        callback:
            context(Simulator)
            (T) -> Unit
    )
}

data class ProgressBar(val label: String, val startTime: Instant, val endTime: Instant) {
    fun duration(): Duration {
        return endTime - startTime
    }

    fun percentageRemaining(currentTime: Instant): Double {
        val totalDuration = endTime - startTime
        val remainingDuration = endTime - currentTime
        val percentageOfTimeRemaining = remainingDuration / totalDuration
        return percentageOfTimeRemaining.coerceAtLeast(0.0)
    }

    fun shouldShow(currentTime: Instant): Boolean {
        return percentageRemaining(currentTime) >= 0.001f
    }
}

interface DisplayProgressBars {
    fun createProgressBar(
        callback:
            context(Simulator)
            (label: String, delay: Duration) -> Unit
    )
}

interface Delay<out T> : Container<T>

interface Match<out MainInputT, out SideInputT, out OutputT> {
    fun onMatch(
        callback:
            context(Simulator)
            (MainInputT, SideInputT, OutputT) -> Unit
    )
}

interface Queue<out T> : Container<T>

interface Service<out T> : BoundedContainer<T> {
    val isServing: Boolean
}

interface LossSink<out T> : Sink<T>

interface OutputSink<out T> : Sink<T>

interface Sink<out T> : Container<T> {
    override fun onLeave(
        callback:
            context(Simulator)
            (T) -> Unit
    ) {}

    override fun supportsResidenceTime() = false
}

interface Split<out InputT, out MainOutputT, out SideOutputT> {
    fun onSplit(
        callback:
            context(Simulator)
            (InputT, MainOutputT, SideOutputT) -> Unit
    )
}
