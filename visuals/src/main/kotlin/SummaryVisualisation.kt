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
import com.group7.metrics.InstantaneousMetric
import com.group7.metrics.MetricGroup
import components.*
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentSet
import utils.GLOBAL_NODE_LABEL
import utils.assignNodeNames

private val EmptyStateHeight = 300.dp

private enum class ChartViewMode(val label: String) {
    Raw("Raw"),
    Average("Average"),
    Histogram("Histogram"),
}

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

@OptIn(ExperimentalMaterial3Api::class)
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

    var selectedMetric by remember(metricIndex) { mutableStateOf(metricIndex.keys.first()) }
    var selectedNodeLabel by
        remember(metricIndex, selectedMetric) { mutableStateOf(metricIndex.getValue(selectedMetric).keys.first()) }
    val groups = metricIndex.getValue(selectedMetric).getValue(selectedNodeLabel)
    val validScenarios = groups.keys.toPersistentSet()
    // Persist user's selection across metric/node changes; effectiveScenarios intersects with what's valid.
    var selectedScenarios by remember { mutableStateOf(validScenarios) }
    val effectiveScenarios =
        remember(selectedScenarios, validScenarios) {
            val intersection = (selectedScenarios intersect validScenarios).toPersistentSet()
            // Reset to all valid if the intersection is empty (e.g. switching to a metric where
            // previously selected scenarios don't exist, or after manually deselecting everything).
            if (intersection.isEmpty()) validScenarios else intersection
        }
    val hasMoments = groups.values.any { it.moments != null }
    val isInstantaneous = groups.values.any { it.raw is InstantaneousMetric }

    val averageHasData =
        hasMoments &&
            groups.entries.any { (simName, group) ->
                group.moments?.mean?.let { mean -> simulations[simName]?.getLatestValue(mean) != null } == true
            }
    val histogramHasData =
        isInstantaneous &&
            groups.entries.any { (simName, group) ->
                simulations[simName]?.let { (it.getHistogram(group.raw)?.totalCount ?: 0) > 0 } == true
            }

    val availableModes = buildList {
        add(ChartViewMode.Raw)
        if (hasMoments) add(ChartViewMode.Average)
        if (isInstantaneous) add(ChartViewMode.Histogram)
    }
    val modeEnabled =
        mapOf(
            ChartViewMode.Raw to true,
            ChartViewMode.Average to averageHasData,
            ChartViewMode.Histogram to histogramHasData,
        )

    // Lifted state: persists across simulation switches for the same metric
    var viewMode by remember { mutableStateOf(ChartViewMode.Average) }
    var showCi by remember { mutableStateOf(true) }

    // Coerce to a valid enabled mode when available modes change
    LaunchedEffect(availableModes, modeEnabled) {
        if (viewMode !in availableModes || modeEnabled[viewMode] == false) {
            viewMode = availableModes.firstOrNull { modeEnabled[it] != false } ?: ChartViewMode.Raw
        }
    }

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

            if (validScenarios.size >= 2) {
                MultiSelectDropdown(
                    label = "Scenarios",
                    options = validScenarios.sorted(),
                    selectedOptions = effectiveScenarios,
                    onSelectionChange = { selectedScenarios = it },
                    optionLabel = { it },
                )
            }

            if (availableModes.size > 1) {
                SingleChoiceSegmentedButtonRow {
                    availableModes.forEachIndexed { index, mode ->
                        val enabled = modeEnabled.getValue(mode)
                        val tooltip =
                            when {
                                enabled -> null
                                mode == ChartViewMode.Histogram -> "Sample never triggered"
                                mode == ChartViewMode.Average -> "Convergence not yet reached"
                                else -> null
                            }
                        val button =
                            @Composable {
                                SegmentedButton(
                                    selected = viewMode == mode,
                                    onClick = { viewMode = mode },
                                    shape = SegmentedButtonDefaults.itemShape(index, availableModes.size),
                                    enabled = enabled,
                                    icon = {},
                                ) {
                                    Text(mode.label)
                                }
                            }
                        if (tooltip != null) {
                            TooltipBox(
                                positionProvider =
                                    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                tooltip = { PlainTooltip { Text(tooltip) } },
                                state = rememberTooltipState(),
                            ) {
                                button()
                            }
                        } else {
                            button()
                        }
                    }
                }
                if (viewMode == ChartViewMode.Average) {
                    LabeledSwitch("Show CI", checked = showCi, onCheckedChange = { showCi = it })
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingLg)) {
            if (effectiveScenarios.isNotEmpty()) {
                val filteredMetrics =
                    metricIndex
                        .getValue(selectedMetric)
                        .getValue(selectedNodeLabel)
                        .filterKeys { it in effectiveScenarios }
                        .toImmutableMap()

                // key(viewMode) tears down and recreates all composition state on view mode switch,
                // so Vico's internal rememberSaveable state (zoom, animation) is discarded and
                // cannot be erroneously restored into the wrong type (e.g. SpringSpec → SaveableHolder).
                key(viewMode) {
                    when (viewMode) {
                        ChartViewMode.Raw ->
                            SummaryChart(
                                metricByScenario = filteredMetrics,
                                simulations = simulations,
                                showRaw = true,
                                showCi = false,
                            )
                        ChartViewMode.Average ->
                            SummaryChart(
                                metricByScenario = filteredMetrics,
                                simulations = simulations,
                                showRaw = false,
                                showCi = showCi,
                            )
                        ChartViewMode.Histogram ->
                            HistogramChart(metricByScenario = filteredMetrics, simulations = simulations)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(EmptyStateHeight), contentAlignment = Alignment.Center) {
                    Text("Select at least one scenario to display chart")
                }
            }
        }
    }
}
