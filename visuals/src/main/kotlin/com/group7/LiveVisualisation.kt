package com.group7

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
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
import com.group7.components.DurationPicker
import com.group7.components.PlaybackSpeedSlider
import com.group7.state.SimulationState
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.time.toJavaInstant
import kotlinx.coroutines.launch

private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS yyyy-MM-dd").withZone(ZoneOffset.UTC)

@Composable
fun LiveVisualisation(
    scenario: Scenario,
    logger: EventLog = EventLog.noop(),
    iconProvider: IconProvider = IconProvider.defaultProvider(),
) {
    val simulation = remember { SimulationState(scenario, iconProvider) }
    val simulator = remember { SimulatorModel(Simulator(logger, scenario, simulation)) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { simulator.run() }

    SimulationTabLayout(
        "Simulation",
        simulation,
        getAnimatableTime = { simulator.currentTime },
        iconProvider = iconProvider,
    ) {
        // Playback controls at bottom - fixed height
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).border(Dimensions.borderWidth, Color.Black),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { simulator.playPause() },
                modifier = Modifier.width(100.dp),
                enabled = !simulator.isStepping,
            ) {
                Text(if (simulator.isRunning) "Pause" else "Play")
            }

            PlaybackSpeedSlider(
                currentSpeed = simulator.playbackSpeed,
                onSpeedChange = { simulator.playbackSpeed = it },
                modifier = Modifier.weight(1f).padding(Dimensions.spacingLg),
            )

            if (simulator.isStepping) {
                Button(onClick = { scope.launch { simulator.stopStepping() } }) { Text("Cancel") }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            simulation.beginBatch()
                            simulator.step(scope)
                            simulation.endBatch()
                        }
                    },
                    enabled = simulator.stepDuration != null,
                ) {
                    Text("Step for:")
                }
            }

            DurationPicker(duration = simulator.stepDuration, onDurationChange = { simulator.stepDuration = it })

            val time = formatter.format(simulator.currentTime.toJavaInstant())
            Text(
                time,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(250.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}
