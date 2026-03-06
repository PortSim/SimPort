package com.group7

import androidx.compose.runtime.Composable
import com.group7.state.SimulationState

/** Top-level composable for a completed (non-animated) simulation, rendered via [SimulationTabLayout]. */
@Composable
fun StaticVisualisation(
    simulation: SimulationState,
    simulationName: String = "Simulation",
    iconProvider: IconProvider?,
) {
    SimulationTabLayout(
        simulationName,
        simulation,
        iconProvider = iconProvider,
        getAnimatableTime = { simulation.progressBarsState.latestTimeSeen },
    )
}
