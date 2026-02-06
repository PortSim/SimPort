import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import com.group7.Scenario
import components.MetricsPanelState

/**
 * MultiVisualisation displays multiple completed simulations.
 *
 * Simulations is a list of triples containing:
 * - A `String` name for the simulation (e.g. the parameter you're changing)
 * - The `Scenario` of the finished simulation
 * - The `MetricsPanelState` containing the samples from the simulation
 */
@Composable
fun MultiVisualisation(simulations: List<Triple<String, Scenario, MetricsPanelState>>) {

    var individualOrSummaryTab by remember { mutableStateOf(0) }

    Column() {
        PrimaryTabRow(selectedTabIndex = individualOrSummaryTab) {
            Tab(
                selected = individualOrSummaryTab == 0,
                onClick = { individualOrSummaryTab = 0 },
                text = { Text("Individual simulations") },
            )
            Tab(
                selected = individualOrSummaryTab == 1,
                onClick = { individualOrSummaryTab = 1 },
                text = { Text("Summary metrics") },
            )
        }

        Box(Modifier.weight(1f).clipToBounds()) {
            when (individualOrSummaryTab) {
                0 -> { // Individual sims

                    var simulationTab by remember { mutableStateOf(0) }

                    Column() {
                        PrimaryTabRow(selectedTabIndex = simulationTab) {
                            simulations.forEachIndexed { index, simulation ->
                                Tab(
                                    selected = simulationTab == index,
                                    onClick = { simulationTab = index },
                                    text = { Text(simulation.first) },
                                )
                            }
                        }

                        StaticVisualisation(simulations[simulationTab].second, simulations[simulationTab].third)
                    }
                }
                1 -> {
                    SummaryVisualisation(simulations)
                }
            }
        }
    }
}
