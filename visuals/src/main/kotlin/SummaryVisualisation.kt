import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.group7.metrics.MetricGroup
import components.MetricsPanelState
import components.SummaryChart
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentSet
import utils.assignNodeNames

private val EmptyStateHeight = 300.dp

@Composable
private fun <T> MultiSelectDropdown(
    label: String,
    options: List<T>,
    selectedOptions: PersistentSet<T>,
    onSelectionChange: (PersistentSet<T>) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Button(onClick = { expanded = true }) { Text("$label (${selectedOptions.size})") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (option in options) {
                val isSelected = option in selectedOptions
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSm),
                        ) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Text(optionLabel(option))
                        }
                    },
                    onClick = {
                        val newSelection =
                            if (isSelected) {
                                selectedOptions.remove(option)
                            } else {
                                selectedOptions.add(option)
                            }
                        onSelectionChange(newSelection)
                    },
                )
            }
        }
    }
}

@Composable
private fun NodeDropdown(
    nodes: Iterable<String?>,
    selectedNode: String?,
    onNodeSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Dropdown(nodes, selectedNode, onNodeSelected, modifier) { option -> Text(option ?: "<no associated node>") }
}

@Composable
private fun <T> Dropdown(
    options: Iterable<T>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable (T) -> Unit = { Text(it?.toString() ?: "Select...") },
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Button(onClick = { expanded = true }) { preview(selected) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (option in options) {
                DropdownMenuItem(
                    text = { preview(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun SummaryVisualisation(simulations: ImmutableMap<String, MetricsPanelState>) {
    if (simulations.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No simulations to display") }
        return
    }
    // metricName -> nodeName? (could be global metric) -> simulationName: metricGroup
    val metricIndex =
        remember(simulations) {
            val result = mutableMapOf<String, MutableMap<String?, MutableMap<String, MetricGroup>>>()

            for ((simulationName, metricsState) in simulations) {
                val nodeNames = assignNodeNames(metricsState.scenario)

                for ((metricName, metricGroups) in metricsState.metricGroups) {
                    for (metricGroup in metricGroups) {
                        result
                            .getOrPut(metricName) { sortedMapOf(nullsFirst()) }
                            .getOrPut(metricGroup.associatedNode?.let(nodeNames::getValue), ::sortedMapOf)[
                                simulationName] = metricGroup
                    }
                }
            }

            result as Map<String, Map<String?, Map<String, MetricGroup>>>
        }

    val allSimulationNames = remember(simulations) { simulations.keys.sorted() }

    var selectedMetric by remember(metricIndex) { mutableStateOf(metricIndex.keys.first()) }
    var selectedNodeLabel by
        remember(metricIndex, selectedMetric) { mutableStateOf(metricIndex.getValue(selectedMetric).keys.first()) }
    var selectedScenarios by
        remember(metricIndex, selectedMetric, selectedNodeLabel) {
            mutableStateOf(metricIndex.getValue(selectedMetric).getValue(selectedNodeLabel).keys.toPersistentSet())
        }
    val groups = metricIndex.getValue(selectedMetric).getValue(selectedNodeLabel)
    var showRaw by remember(metricIndex, selectedMetric) { mutableStateOf(false) }
    val hasMoments = groups.values.any { it.moments != null }

    Column(
        Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(Dimensions.spacingLg),
        verticalArrangement = Arrangement.spacedBy(Dimensions.spacingLg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingLg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dropdown(options = metricIndex.keys, selected = selectedMetric, onSelected = { selectedMetric = it }) {
                option ->
                Text(option)
            }

            NodeDropdown(
                nodes = metricIndex.getValue(selectedMetric).keys,
                selectedNode = selectedNodeLabel,
                onNodeSelected = { selectedNodeLabel = it },
            )

            if (allSimulationNames.size >= 2) {
                MultiSelectDropdown(
                    label = "Scenarios",
                    options = allSimulationNames,
                    selectedOptions = selectedScenarios,
                    onSelectionChange = { selectedScenarios = it },
                    optionLabel = { it },
                )
            }

            if (hasMoments) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Show Raw", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Switch(checked = showRaw, onCheckedChange = { showRaw = it }, modifier = Modifier.scale(0.6f))
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingLg)) {
            if (selectedScenarios.isNotEmpty()) {
                SummaryChart(
                    metricByScenario =
                        metricIndex
                            .getValue(selectedMetric)
                            .getValue(selectedNodeLabel)
                            .filterKeys { it in selectedScenarios }
                            .toImmutableMap(),
                    simulations = simulations,
                    showRaw = showRaw || !hasMoments,
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(EmptyStateHeight), contentAlignment = Alignment.Center) {
                    Text("Select at least one scenario to display chart")
                }
            }
        }
    }
}
