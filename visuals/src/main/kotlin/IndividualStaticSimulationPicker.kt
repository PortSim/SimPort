import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import components.MetricsPanelState
import kotlinx.collections.immutable.ImmutableMap

private object SimPickerDimensions {
    val dividerHeight = 32.dp
    val surfaceElevation = 2.dp
}

@Composable
fun IndividualStaticSimulationPicker(simulations: ImmutableMap<String, MetricsPanelState>) {
    val simulationNames = simulations.keys.sorted()
    var simulationTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            StaticVisualisation(simulations.getValue(simulationNames[simulationTab]))
        }

        HorizontalDivider()

        SimulationPickerBar(
            simulations = simulationNames,
            selectedIndex = simulationTab,
            onSelectionChange = { simulationTab = it },
        )
    }
}

@Composable
private fun SimulationPickerBar(simulations: List<String>, selectedIndex: Int, onSelectionChange: (Int) -> Unit) {
    Surface(tonalElevation = SimPickerDimensions.surfaceElevation) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(Dimensions.controlBarHeight),
        ) {
            SimulationDropdownMenu(
                simulations = simulations,
                selectedIndex = selectedIndex,
                onSelectionChange = onSelectionChange,
            )

            PickerVerticalDivider()

            SimulationChipRow(
                simulations = simulations,
                selectedIndex = selectedIndex,
                onSelectionChange = onSelectionChange,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SimulationDropdownMenu(simulations: List<String>, selectedIndex: Int, onSelectionChange: (Int) -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { menuExpanded = true }) {
            Icon(Icons.Default.Menu, contentDescription = "Select simulation")
            Spacer(Modifier.width(Dimensions.spacingXs))
            Text("Simulation")
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            simulations.forEachIndexed { index, simulation ->
                DropdownMenuItem(
                    text = { Text(simulation) },
                    onClick = {
                        onSelectionChange(index)
                        menuExpanded = false
                    },
                    leadingIcon =
                        if (selectedIndex == index) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                )
            }
        }
    }
}

@Composable
private fun PickerVerticalDivider() {
    Box(
        modifier =
            Modifier.width(Dimensions.borderWidth)
                .height(SimPickerDimensions.dividerHeight)
                .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun SimulationChipRow(
    simulations: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
        simulations.forEachIndexed { index, simulation ->
            SimulationChip(name = simulation, selected = selectedIndex == index, onClick = { onSelectionChange(index) })
        }
    }
}

@Composable
private fun SimulationChip(name: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(horizontal = Dimensions.spacingXxs),
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = Dimensions.spacingMd, vertical = Dimensions.spacingSm),
            style = MaterialTheme.typography.labelMedium,
            color =
                if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
