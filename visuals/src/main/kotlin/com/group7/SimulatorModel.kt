package com.group7

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class SimulatorModel(private val simulator: Simulator) {
    var isRunning by mutableStateOf(false)
        private set

    var playbackSpeed by mutableFloatStateOf(1f)

    var stepDuration by mutableStateOf<Duration?>(1.seconds)

    private var lastTimeBeforeStepping = simulator.currentTime
    private var currentBaseTime by mutableStateOf(simulator.currentTime)
    private var nextEventTime by mutableStateOf<Instant?>(null)
    private val progress = Animatable(0f)
    private var stepJob by mutableStateOf<Job?>(null)
    private var runToken by mutableIntStateOf(0)
    private var runJob: Job? = null

    var isStepping: Boolean by mutableStateOf(false)
        private set

    val currentTime
        get() =
            if (nextEventTime != null) {
                val stepDuration = nextEventTime!! - currentBaseTime
                currentBaseTime + stepDuration * progress.value.toDouble()
            } else {
                currentBaseTime
            }

    val progressBarsTime
        get() =
            if (isStepping) {
                lastTimeBeforeStepping
            } else {
                currentTime
            }

    suspend fun run() {
        snapshotFlow { Triple(isRunning, playbackSpeed, runToken) }
            .collectLatest { (isRunning, playbackSpeed) ->
                updateBaseTime()
                if (isRunning) {
                    coroutineScope {
                        runJob = launch {
                            while (!simulator.isFinished) {
                                nextEventTime = simulator.nextEventTime!!
                                val delay = ((nextEventTime!! - currentBaseTime) / playbackSpeed.toDouble())
                                progress.snapTo(0f)
                                progress.animateTo(
                                    1f,
                                    animationSpec =
                                        tween(
                                            durationMillis = delay.toDouble(DurationUnit.MILLISECONDS).toInt(),
                                            easing = LinearEasing,
                                        ),
                                )
                                simulator.nextStep()
                                nextEventTime = null
                                currentBaseTime = simulator.currentTime
                            }
                            this@SimulatorModel.isRunning = false
                        }
                    }
                }
            }
    }

    fun playPause() {
        if (!simulator.isFinished) {
            isRunning = !isRunning
        }
    }

    suspend fun updateBaseTime() {
        currentBaseTime = currentTime
        nextEventTime = null
        progress.snapTo(0f)
    }

    suspend fun step(scope: CoroutineScope) {
        val duration = stepDuration ?: return
        isStepping = true
        runJob?.cancelAndJoin()
        lastTimeBeforeStepping = simulator.currentTime
        // this leads to an exception in run
        updateBaseTime()
        val endTime = currentBaseTime + duration
        scope
            .launch(Dispatchers.Default) {
                try {
                    while (!simulator.isFinished) {
                        if (simulator.nextEventTime!! > endTime) {
                            break
                        }
                        currentCoroutineContext().ensureActive()
                        simulator.nextStep()
                        currentBaseTime = simulator.currentTime
                    }
                    currentBaseTime = endTime
                } finally {
                    if (!simulator.isFinished) {
                        runToken++
                    }
                    isStepping = false
                }
            }
            .let {
                stepJob = it
                it.join()
            }
    }

    suspend fun stopStepping() {
        stepJob?.cancelAndJoin()
    }
}
