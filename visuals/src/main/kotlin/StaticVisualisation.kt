import androidx.compose.runtime.Composable
import components.MetricsPanelState

@Composable
fun StaticVisualisation(metricsPanelState: MetricsPanelState, simulationName: String = "Simulation") {
    SimulationTabLayout(simulationName, metricsPanelState)
}
