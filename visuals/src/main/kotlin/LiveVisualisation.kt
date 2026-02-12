import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.group7.EventLog
import com.group7.Scenario
import com.group7.Simulator
import components.*
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.time.toJavaInstant
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch

private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS yyyy-MM-dd").withZone(ZoneOffset.UTC)

@Composable
fun LiveVisualisation(scenario: Scenario) {
    val metricsPanelState = remember { MetricsPanelState(scenario) }
    val simulation = remember { SimulationModel(Simulator(EventLog.noop(), scenario, metricsPanelState)) }
    val scenarioLayout = remember { ScenarioLayout(scenario) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { simulation.run { scenarioLayout.refresh() } }

    var selectedTab by remember { mutableStateOf(0) }
    var showDebug by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
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

        // Tab Row at the top
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Graph Viewer") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Metrics") })
        }

        // Content area - takes remaining space
        Box(Modifier.weight(1f).clipToBounds()) {
            when (selectedTab) {
                0 -> SimpleGraphViewer(scenarioLayout)
                1 -> SummaryVisualisation(persistentMapOf("Simulation" to metricsPanelState))
            }
        }

        // Playback controls at bottom - fixed height
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).border(Dimensions.borderWidth, Color.Black),
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
                            metricsPanelState.beginBatch()
                            simulation.step(scope)
                            metricsPanelState.endBatch()
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
