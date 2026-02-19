import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import components.MetricsPanelState
import components.ResultsTablePage
import components.SimpleGraphViewer
import components.debugPanel
import kotlinx.collections.immutable.persistentMapOf

enum class SimulationTab(val label: String) {
    GraphViewer("Graph Viewer"),
    Metrics("Metrics"),
    ResultsTable("Results Table"),
}

@Composable
fun SimulationTabLayout(
    simulationName: String,
    metricsPanelState: MetricsPanelState,
    scenarioLayout: ScenarioLayout,
    showResultsToolbar: Boolean = false,
    bottomBar: @Composable ColumnScope.() -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(SimulationTab.GraphViewer) }
    var showDebug by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        Modifier.fillMaxSize().focusRequester(focusRequester).focusable().onKeyEvent { event ->
            if (event.key == Key.D && event.type == KeyEventType.KeyUp) {
                showDebug = !showDebug
                true
            } else {
                false
            }
        }
    ) {
        if (showDebug) {
            debugPanel()
        }

        SecondaryTabRow(selectedTabIndex = selectedTab.ordinal) {
            SimulationTab.entries.forEach { tab ->
                Tab(selected = selectedTab == tab, onClick = { selectedTab = tab }, text = { Text(tab.label) })
            }
        }

        val simulations = persistentMapOf(simulationName to metricsPanelState)
        Box(Modifier.weight(1f).clipToBounds()) {
            when (selectedTab) {
                SimulationTab.GraphViewer -> SimpleGraphViewer(simulationName, metricsPanelState, scenarioLayout)
                SimulationTab.Metrics -> SummaryVisualisation(simulations)
                SimulationTab.ResultsTable -> ResultsTablePage(simulations, showToolbar = showResultsToolbar)
            }
        }
        // For live visualisation playback controls
        bottomBar()
    }
}
