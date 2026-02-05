import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import com.group7.Simulator
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class SimulationModel(private val simulator: Simulator) {
    var isRunning by mutableStateOf(false)
        private set

    var playbackSpeed by mutableFloatStateOf(1f)

    var stepDuration by mutableStateOf<Duration?>(1.seconds)

    private var currentBaseTime by mutableLongStateOf(simulator.currentTime.toEpochMilliseconds())
    private var nextEventTime by mutableStateOf<Long?>(null)
    private val progress = Animatable(0f)
    private var stepJob by mutableStateOf<Job?>(null)
    private var runToken by mutableIntStateOf(0)
    private var runJob: Job? = null

    val isStepping
        get() = stepJob != null

    val currentTime
        get() =
            Instant.fromEpochMilliseconds(
                currentBaseTime +
                    (nextEventTime?.let { nextTime -> ((nextTime - currentBaseTime) * progress.value).roundToLong() }
                        ?: 0L)
            )

    suspend fun run(onStep: () -> Unit = {}) {
        snapshotFlow { Triple(isRunning, playbackSpeed, runToken) }
            .collectLatest { (isRunning, playbackSpeed) ->
                updateBaseTime()
                if (isRunning) {
                    coroutineScope {
                        runJob = launch {
                            while (!simulator.isFinished) {
                                nextEventTime = simulator.nextEventTime!!.toEpochMilliseconds()
                                val delay = ((nextEventTime!! - currentBaseTime) / playbackSpeed).roundToInt()
                                progress.snapTo(0f)
                                progress.animateTo(
                                    1f,
                                    animationSpec = tween(durationMillis = delay, easing = LinearEasing),
                                )
                                simulator.nextStep()
                                nextEventTime = null
                                currentBaseTime = simulator.currentTime.toEpochMilliseconds()
                                onStep()
                            }
                            this@SimulationModel.isRunning = false
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
        currentBaseTime = currentTime.toEpochMilliseconds()
        nextEventTime = null
        progress.snapTo(0f)
    }

    suspend fun step(scope: CoroutineScope) {
        runJob?.cancelAndJoin()
        val duration = stepDuration ?: return
        // this leads to an exception in run
        updateBaseTime()
        val endTime = Instant.fromEpochMilliseconds(currentBaseTime) + duration
        stepJob =
            scope.launch(Dispatchers.Default) {
                try {
                    while (!simulator.isFinished) {
                        if (simulator.nextEventTime!! > endTime) {
                            break
                        }
                        currentCoroutineContext().ensureActive()
                        simulator.nextStep()
                        currentBaseTime = simulator.currentTime.toEpochMilliseconds()
                    }
                    currentBaseTime = endTime.toEpochMilliseconds()
                } finally {
                    if (!simulator.isFinished) {
                        runToken++
                    }
                }
            }
        stepJob?.join()
        stepJob = null
    }

    suspend fun stopStepping() {
        stepJob?.cancelAndJoin()
    }
}
