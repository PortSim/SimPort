import androidx.compose.runtime.Composable
import components.MetricsPanelState

@Composable
fun StaticVisualisation(
    metricsPanelState: MetricsPanelState,
    simulationName: String = "Simulation",
    iconProvider: IconProvider?,
) {
    SimulationTabLayout(
        simulationName,
        metricsPanelState,
        iconProvider = iconProvider,
        getAnimatableTime = { metricsPanelState.latestTimeDisplayed },
    )
}
