import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.group7.EventLog
import com.group7.Scenario
import com.group7.Simulator
import components.MetricsPanelState
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS yyyy-MM-dd").withZone(ZoneOffset.UTC)

@Composable
fun LiveVisualisation(scenario: Scenario, logger: EventLog = EventLog.noop()) {
    val metricsPanelState = remember { MetricsPanelState(scenario) }
    val simulation = remember { SimulationModel(Simulator(logger, scenario, metricsPanelState)) }
    val scenarioLayout = remember { ScenarioLayout(scenario) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { simulation.run { scenarioLayout.refresh() } }

    // TODO not sure how we should do this, i think we should have a central class to control all compose
    // simulations state something like merging metricsPanelState with ScenarioLayoutState so there's a central
    // refresh thing to call
    //    SimulationTabLayout(
    //        scenarioLayout = scenarioLayout,
    //        simulations = persistentMapOf("Simulation" to metricsPanelState),
    //    ) {
    //        // Playback controls at bottom - fixed height
    //        Row(
    //            modifier = Modifier.fillMaxWidth().background(Color.White).border(Dimensions.borderWidth,
    // Color.Black),
    //            verticalAlignment = Alignment.CenterVertically,
    //        ) {
    //            Button(
    //                onClick = { simulation.playPause() },
    //                modifier = Modifier.width(100.dp),
    //                enabled = !simulation.isStepping,
    //            ) {
    //                Text(if (simulation.isRunning) "Pause" else "Play")
    //            }
    //
    //            PlaybackSpeedSlider(
    //                currentSpeed = simulation.playbackSpeed,
    //                onSpeedChange = { simulation.playbackSpeed = it },
    //                modifier = Modifier.weight(1f),
    //            )
    //
    //            if (simulation.isStepping) {
    //                Button(onClick = { scope.launch { simulation.stopStepping() } }) { Text("Cancel") }
    //            } else {
    //                Button(
    //                    onClick = {
    //                        scope.launch {
    //                            metricsPanelState.beginBatch()
    //                            simulation.step(scope)
    //                            metricsPanelState.endBatch()
    //                            scenarioLayout.refresh()
    //                        }
    //                    },
    //                    enabled = simulation.stepDuration != null,
    //                ) {
    //                    Text("Step for:")
    //                }
    //            }
    //
    //            DurationPicker(duration = simulation.stepDuration, onDurationChange = { simulation.stepDuration = it
    // })
    //
    //            val time = formatter.format(simulation.currentTime.toJavaInstant())
    //            Text(
    //                time,
    //                fontFamily = FontFamily.Monospace,
    //                modifier = Modifier.width(250.dp),
    //                textAlign = TextAlign.Center,
    //            )
    //        }
    //    }
}
