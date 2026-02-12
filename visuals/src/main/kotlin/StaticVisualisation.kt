import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import components.MetricsPanelState
import components.SimpleGraphViewer
import components.debugPanel
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.collections.immutable.persistentMapOf

private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS yyyy-MM-dd").withZone(ZoneOffset.UTC)

@Composable
fun StaticVisualisation(metricsPanelState: MetricsPanelState) {
    val scenarioLayout = remember(metricsPanelState) { ScenarioLayout(metricsPanelState.scenario) }

    var selectedTab by remember { mutableStateOf(0) }
    var showDebug by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

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

        // Tab Row at the top
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Graph Viewer") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Metrics") })
        }

        // Content area - takes remaining space
        Box(Modifier.weight(1f).clipToBounds()) {
            when (selectedTab) {
                0 -> SimpleGraphViewer(scenarioLayout)
                1 -> SummaryVisualisation(persistentMapOf("Simulation" to metricsPanelState))
            }
        }
    }
}
