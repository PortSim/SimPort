import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import components.MetricsPanelState
import kotlinx.collections.immutable.persistentMapOf

@Composable
fun StaticVisualisation(metricsPanelState: MetricsPanelState, simulationName: String = "Simulation") {
    val scenarioLayout = remember(metricsPanelState) { ScenarioLayout(metricsPanelState.scenario) }

    SimulationTabLayout(
        scenarioLayout = scenarioLayout,
        simulations = persistentMapOf(simulationName to metricsPanelState),
    )
}
