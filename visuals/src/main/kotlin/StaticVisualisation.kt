import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import components.MetricsPanelState

@Composable
fun StaticVisualisation(metricsPanelState: MetricsPanelState, simulationName: String = "Simulation") {
    val scenarioLayout = remember { ScenarioLayout(metricsPanelState.scenario) }
    SimulationTabLayout(simulationName, metricsPanelState, scenarioLayout)
}
