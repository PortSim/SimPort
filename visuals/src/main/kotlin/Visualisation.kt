import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.group7.EventLog
import com.group7.Scenario
import com.group7.Simulator
import components.DurationPicker
import components.PlaybackSpeedSlider
import components.SimpleGraphViewer
import components.debugPanel
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.time.toJavaInstant
import kotlinx.coroutines.launch

private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS yyyy-MM-dd").withZone(ZoneOffset.UTC)

@Composable
fun Visualisation(scenario: Scenario) {
    val simulation = remember { SimulationModel(Simulator(EventLog.noop(), scenario)) }
    val scenarioLayout = remember { ScenarioLayout(scenario) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { simulation.run { scenarioLayout.refresh() } }

    var showDebug by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        Modifier.fillMaxSize().focusRequester(focusRequester).focusable().onKeyEvent { event ->
            if (event.key == Key.D && event.type == KeyEventType.KeyUp) {
                showDebug = !showDebug
                true
            } else {
                false
            }
        }
    ) {
        if (showDebug) {
            debugPanel()
        }

        SimpleGraphViewer(scenarioLayout)

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).background(Color.White).border(1.dp, Color.Black),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { simulation.playPause() },
                modifier = Modifier.width(100.dp),
                enabled = !simulation.isStepping,
            ) {
                Text(if (simulation.isRunning) "Pause" else "Play")
            }

            PlaybackSpeedSlider(
                currentSpeed = simulation.playbackSpeed,
                onSpeedChange = { simulation.playbackSpeed = it },
                modifier = Modifier.weight(1f),
            )

            if (simulation.isStepping) {
                Button(onClick = { scope.launch { simulation.stopStepping() } }) { Text("Cancel") }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            simulation.step(scope)
                            scenarioLayout.refresh()
                        }
                    },
                    enabled = simulation.stepDuration != null,
                ) {
                    Text("Step for:")
                }
            }

            DurationPicker(duration = simulation.stepDuration, onDurationChange = { simulation.stepDuration = it })

            val time = formatter.format(simulation.currentTime.toJavaInstant())
            Text(
                time,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(250.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}
