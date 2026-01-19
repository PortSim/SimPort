import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dk.kuiver.model.layout.LayoutConfig
import com.dk.kuiver.model.layout.LayoutDirection
import com.dk.kuiver.rememberKuiverViewerState
import com.dk.kuiver.renderer.KuiverViewer
import com.dk.kuiver.renderer.KuiverViewerConfig
import com.dk.kuiver.ui.EdgeContent
import com.group7.EventLog
import com.group7.Scenario
import com.group7.Simulator
import components.AutoSizedText
import components.DurationPicker
import components.PlaybackSpeedSlider
import kotlinx.coroutines.launch

@Composable
fun Visualisation(scenario: Scenario) {
    val simulation = remember { SimulationModel(Simulator(EventLog.noop(), scenario)) }
    val model = remember { VisualisationModel(scenario) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { simulation.run { model.refresh() } }

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.weight(1f)) { Graph(model) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(0.75f),
        ) {
            Button(
                onClick = { simulation.playPause() },
                modifier = Modifier.width(100.dp),
                enabled = !simulation.isStepping,
            ) {
                Text(if (simulation.isRunning) "Pause" else "Play")
            }

            if (simulation.isStepping) {
                Button(onClick = { simulation.stopStepping() }) { Text("Cancel") }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            simulation.step(scope)
                            model.refresh()
                        }
                    },
                    enabled = simulation.stepDuration != null,
                ) {
                    Text("Step for:")
                }
            }

            DurationPicker(duration = simulation.stepDuration, onDurationChange = { simulation.stepDuration = it })

            PlaybackSpeedSlider(
                currentSpeed = simulation.playbackSpeed,
                onSpeedChange = { simulation.playbackSpeed = it },
                modifier = Modifier.weight(1f),
            )

            Text(simulation.currentTime.toString(), fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun Graph(model: VisualisationModel) {
    // Configure layout
    val layoutConfig = LayoutConfig.Hierarchical(direction = LayoutDirection.HORIZONTAL)

    // Create viewer state
    val viewerState = rememberKuiverViewerState(initialKuiver = model.kuiver, layoutConfig = layoutConfig)

    // Render the graph
    KuiverViewer(
        config = KuiverViewerConfig(offsetAnimationSpec = snap(), scaleAnimationSpec = snap()),
        state = viewerState,
        nodeContent = { node ->
            // Customize node appearance
            Column(modifier = Modifier.background(Color.Blue).fillMaxSize().padding(3.dp)) {
                AutoSizedText(
                    model.textForNode(node),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                model.metricsForNode(node).occupants?.let { occupants ->
                    Text(
                        occupants.toString(),
                        color = Color.Green,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 14.sp,
                    )
                }
            }
        },
        edgeContent = { edge, from, to ->
            // Customize edge appearance
            EdgeContent(from, to, color = if (model.isChannelOpen(edge)) Color.Green else Color.Red)
        },
    )
}
