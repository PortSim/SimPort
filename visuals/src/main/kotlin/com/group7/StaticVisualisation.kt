package com.group7

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.group7.state.SimulationState

/** Top-level composable for a completed (non-animated) simulation, rendered via [SimulationTabLayout]. */
@Composable
fun StaticVisualisation(simulation: SimulationState, simulationName: String = "Simulation") {
    SimulationTabLayout(
        simulationName,
        simulation,
        getAnimatableTime = { simulation.progressBarsState.latestTimeSeen },
        stepUnit = mutableStateOf(null),
    )
}
