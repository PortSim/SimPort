import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.group7.Simulator
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class SimulationModel(private val simulator: Simulator) {
    var isRunning by mutableStateOf(false)
        private set

    var playbackSpeed by mutableFloatStateOf(1f)

    var stepDuration by mutableStateOf<Duration?>(1.seconds)

    private var currentBaseTime by mutableLongStateOf(simulator.currentTime.toEpochMilliseconds())
    private var nextEventTime by mutableStateOf<Long?>(null)
    private val progress = Animatable(0f)
    private var stepJob by mutableStateOf<Job?>(null)

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
        snapshotFlow { isRunning to playbackSpeed }
            .collectLatest { (isRunning, playbackSpeed) ->
                nextEventTime?.let { nextTime ->
                    currentBaseTime += ((nextTime - currentBaseTime) * progress.value).roundToLong()
                }
                progress.snapTo(0f)

                if (isRunning) {
                    while (!simulator.isFinished) {
                        nextEventTime = simulator.nextEventTime!!.toEpochMilliseconds()
                        val delay = ((nextEventTime!! - currentBaseTime) / playbackSpeed).roundToInt()
                        progress.snapTo(0f)
                        progress.animateTo(1f, animationSpec = tween(durationMillis = delay, easing = LinearEasing))
                        simulator.nextStep()
                        nextEventTime = null
                        currentBaseTime = simulator.currentTime.toEpochMilliseconds()
                        onStep()
                    }
                    this.isRunning = false
                }
            }
    }

    fun playPause() {
        if (!simulator.isFinished) {
            isRunning = !isRunning
        }
    }

    suspend fun step(scope: CoroutineScope) {
        stepJob?.cancel()
        val duration = stepDuration ?: return
        stepJob =
            scope.launch(Dispatchers.Default) {
                nextEventTime?.let { nextTime ->
                    currentBaseTime += ((nextTime - currentBaseTime) * progress.value).roundToLong()
                }
                progress.snapTo(0f)

                val endTime = Instant.fromEpochMilliseconds(currentBaseTime) + duration

                while (!simulator.isFinished) {
                    if (simulator.nextEventTime!! > endTime) {
                        break
                    }
                    currentCoroutineContext().ensureActive()
                    simulator.nextStep()
                    currentBaseTime = simulator.currentTime.toEpochMilliseconds()
                }
                if (simulator.isFinished) {
                    isRunning = false
                }

                currentBaseTime = endTime.toEpochMilliseconds()
            }
        stepJob?.join()
        stepJob = null
    }

    fun stopStepping() {
        stepJob?.cancel()
    }
}
