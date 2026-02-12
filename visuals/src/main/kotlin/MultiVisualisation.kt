import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds

/** MultiVisualisation displays multiple completed simulations. */
@Composable
fun MultiVisualisation(simulations: List<SimulationResult>) {

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
                    IndividualStaticSimulationPicker(simulations)
                }
                1 -> {
                    SummaryVisualisation(simulations)
                }
            }
        }
    }
}
