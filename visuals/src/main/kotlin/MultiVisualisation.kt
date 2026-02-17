import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import components.MetricsPanelState
import components.ResultsTablePage
import kotlinx.collections.immutable.ImmutableMap

/** MultiVisualisation displays multiple completed simulations. */
@Composable
fun MultiVisualisation(simulations: ImmutableMap<String, MetricsPanelState>) {

    var topTab by remember { mutableStateOf(0) }

    Column {
        PrimaryTabRow(selectedTabIndex = topTab) {
            Tab(selected = topTab == 0, onClick = { topTab = 0 }, text = { Text("Individual simulations") })
            Tab(selected = topTab == 1, onClick = { topTab = 1 }, text = { Text("Summary") })
        }

        Box(Modifier.weight(1f).clipToBounds()) {
            when (topTab) {
                0 -> IndividualStaticSimulationPicker(simulations)
                1 -> SummarySubPage(simulations)
            }
        }
    }
}

@Composable
private fun SummarySubPage(simulations: ImmutableMap<String, MetricsPanelState>) {
    var subTab by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = subTab) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("Metrics Chart") })
            Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text("Results Table") })
        }

        Box(Modifier.weight(1f).clipToBounds()) {
            when (subTab) {
                0 -> SummaryVisualisation(simulations)
                1 -> ResultsTablePage(simulations)
            }
        }
    }
}
