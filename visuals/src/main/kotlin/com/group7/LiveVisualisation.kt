package com.group7

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
private fun PlaybackVerticalDivider() {
    Box(
        modifier =
            Modifier.width(Dimensions.borderWidth)
                .height(Dimensions.playbackDividerHeight)
                .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
fun LiveVisualisation(
    scenario: Scenario,
    logger: EventLog = EventLog.noop(),
    iconProvider: IconProvider = IconProvider.defaultProvider(),
) {
    val simulation = remember { SimulationState(scenario, iconProvider) }
    val simulator = remember { SimulatorModel(Simulator(logger, scenario, simulation)) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        launch { simulator.run() }
        launch { simulation.updateLive() }
    }

    SimulationTabLayout(
        "Simulation",
        simulation,
        getAnimatableTime = { simulator.progressBarsTime },
        iconProvider = iconProvider,
    ) {
        HorizontalDivider()

        Surface(tonalElevation = Dimensions.playbackSurfaceElevation) {
            Row(
                modifier =
                    Modifier.fillMaxWidth().padding(horizontal = Dimensions.spacingLg, vertical = Dimensions.spacingSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSm),
            ) {
                val time = formatter.format(simulator.currentTime.toJavaInstant())
                Column {
                    Text(
                        "Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        time,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }

                PlaybackVerticalDivider()

                Button(
                    onClick = { simulator.playPause() },
                    modifier = Modifier.width(100.dp),
                    enabled = !simulator.isStepping,
                ) {
                    Text(if (simulator.isRunning) "Pause" else "Play")
                }

                PlaybackVerticalDivider()

                PlaybackSpeedSlider(
                    currentSpeed = simulator.playbackSpeed,
                    onSpeedChange = { simulator.playbackSpeed = it },
                    modifier = Modifier.weight(1f),
                )

                PlaybackVerticalDivider()

                Box(modifier = Modifier.width(IntrinsicSize.Max)) {
                    // Invisible "Step for:" to reserve the wider size
                    Button(onClick = {}, modifier = Modifier.alpha(0f)) { Text("Step for:") }
                    if (simulator.isStepping) {
                        Button(
                            onClick = { scope.launch { simulator.stopStepping() } },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Cancel")
                        }
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
                }

                DurationPicker(duration = simulator.stepDuration, onDurationChange = { simulator.stepDuration = it })
            }
        }
    }
}
