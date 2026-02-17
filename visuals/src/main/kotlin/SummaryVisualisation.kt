import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.group7.metrics.MetricGroup
import components.Dropdown
import components.LabeledSwitch
import components.MetricsPanelState
import components.SummaryChart
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentSet
import utils.GLOBAL_NODE_LABEL
import utils.assignNodeNames

private val EmptyStateHeight = 300.dp

@OptIn(ExperimentalMaterial3Api::class)
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

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = "$label (${selectedOptions.size})",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors =
                OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            modifier =
                Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .pointerHoverIcon(PointerIcon.Default, overrideDescendants = true),
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
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
    var showCi by remember(metricIndex, selectedMetric) { mutableStateOf(true) }
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
            Dropdown(
                options = metricIndex.keys,
                selected = selectedMetric,
                onSelected = { selectedMetric = it },
                label = { Text("Metric") },
            )

            // Node dropdown
            Dropdown(
                options = metricIndex.getValue(selectedMetric).keys,
                selected = selectedNodeLabel,
                onSelected = { selectedNodeLabel = it },
                label = { Text("Node") },
                displayText = { it ?: GLOBAL_NODE_LABEL },
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
                LabeledSwitch("Show Raw", checked = showRaw, onCheckedChange = { showRaw = it })
                LabeledSwitch("Show CI", checked = showCi, onCheckedChange = { showCi = it }, enabled = !showRaw)
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
                    showCi = showCi && !showRaw && hasMoments,
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(EmptyStateHeight), contentAlignment = Alignment.Center) {
                    Text("Select at least one scenario to display chart")
                }
            }
        }
    }
}
