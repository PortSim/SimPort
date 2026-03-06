package com.group7

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.group7.components.*
import com.group7.metrics.InstantaneousMetric
import com.group7.metrics.MetricGroup
import com.group7.state.SimulationState
import com.group7.utils.GLOBAL_NODE_LABEL
import com.group7.utils.assignNodeNames
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentSet

private val EmptyStateHeight = 300.dp

private enum class ChartViewMode(val label: String) {
    Raw("Raw"),
    Average("Average"),
    Histogram("Histogram"),
}

/**
 * Segmented button row for selecting the chart view mode (Raw / Average / Histogram). Disabled modes show a tooltip
 * explaining why they are unavailable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewModeSelector(
    availableModes: List<ChartViewMode>,
    modeEnabled: Map<ChartViewMode, Boolean>,
    viewMode: ChartViewMode,
    onViewModeChange: (ChartViewMode) -> Unit,
) {
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
                        onClick = { onViewModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, availableModes.size),
                        enabled = enabled,
                        icon = {},
                    ) {
                        Text(mode.label)
                    }
                }
            if (tooltip != null) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
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
}

/** Controls specific to Histogram view mode: bin count slider with auto-reset, probability toggle, and log X toggle. */
@Composable
private fun FlowRowScope.HistogramControls(
    numBins: Int?,
    onNumBinsChange: (Int?) -> Unit,
    showDensity: Boolean,
    onShowDensityChange: (Boolean) -> Unit,
    logScale: Boolean,
    onLogScaleChange: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.weight(1f).widthIn(min = 250.dp), verticalAlignment = Alignment.CenterVertically) {
        LabeledSlider(
            // set slider to 25 (1/4 visually pleasing) for Auto value
            value = (numBins ?: 25).toFloat(),
            onValueChange = { onNumBinsChange(it.roundToInt()) },
            valueRange = 3f..100f,
            steps = 96,
            minLabel = "3",
            maxLabel = "100",
            valueLabel = "Bins: ${numBins ?: "Auto"}",
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = { onNumBinsChange(null) },
            enabled = numBins != null,
            modifier = Modifier.alpha(if (numBins != null) 1f else 0f),
        ) {
            Text("Auto")
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSm)) {
        LabeledSwitch("Probability", checked = showDensity, onCheckedChange = onShowDensityChange)
        LabeledSwitch("Log X", checked = logScale, onCheckedChange = onLogScaleChange)
    }
}

/**
 * Aggregated metrics view for one or more simulations.
 *
 * Builds a three-level index (metric → node → simulation) from the simulation data and lets the user select a metric,
 * node, and scenario subset. Supports three chart modes: Raw time-series, Average (with optional CI bands), and
 * Histogram (with configurable bins and density scaling).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryVisualisation(simulations: ImmutableMap<String, SimulationState>) {
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

    if (metricIndex.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No metrics being tracked") }
        return
    }

    var selectedMetric by remember(metricIndex) { mutableStateOf(metricIndex.keys.first()) }
    var selectedNodeLabel by
        remember(metricIndex, selectedMetric) { mutableStateOf(metricIndex.getValue(selectedMetric).keys.first()) }
    val groups = metricIndex.getValue(selectedMetric).getValue(selectedNodeLabel)
    val validScenarios = groups.keys.toPersistentSet()
    // Reset selection when valid scenarios change (e.g. switching simulator).
    var selectedScenarios by remember(validScenarios) { mutableStateOf(validScenarios) }
    val effectiveScenarios =
        remember(selectedScenarios, validScenarios) { (selectedScenarios intersect validScenarios).toPersistentSet() }
    val hasRaw = groups.values.any { it.raw != null }
    val hasMoments = groups.values.any { it.moments != null }
    val isInstantaneous = groups.values.any { it.raw is InstantaneousMetric }

    val averageHasData =
        hasMoments &&
            groups.entries.any { (simName, group) ->
                group.moments?.mean?.let { mean -> simulations[simName]?.chartsState?.getLatestValue(mean) != null } ==
                    true
            }
    val histogramHasData =
        isInstantaneous &&
            groups.entries.any { (simName, group) ->
                simulations[simName]?.let { metricsPanelState ->
                    (group.raw?.let { metricsPanelState.histogramsState.getHistogram(it) }?.totalCount ?: 0) > 0
                } == true
            }

    val availableModes = buildList {
        if (hasRaw) add(ChartViewMode.Raw)
        if (hasMoments) add(ChartViewMode.Average)
        if (isInstantaneous) add(ChartViewMode.Histogram)
    }
    val modeEnabled =
        mapOf(
            ChartViewMode.Raw to hasRaw,
            ChartViewMode.Average to averageHasData,
            ChartViewMode.Histogram to histogramHasData,
        )

    // Lifted state: persists across simulation switches for the same metric
    var viewMode by remember { mutableStateOf(ChartViewMode.Average) }
    var showCi by remember { mutableStateOf(true) }
    var numBins by remember { mutableStateOf<Int?>(null) } // null = auto (Sturges' rule)
    var showDensity by remember { mutableStateOf(false) }
    var logScale by remember { mutableStateOf(false) }

    // Coerce to a valid enabled mode when available modes change
    LaunchedEffect(availableModes, modeEnabled) {
        if (viewMode !in availableModes || modeEnabled[viewMode] == false) {
            viewMode = availableModes.firstOrNull { modeEnabled[it] != false } ?: ChartViewMode.Raw
        }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow).padding(Dimensions.spacingMd),
        verticalArrangement = Arrangement.spacedBy(Dimensions.spacingSm),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSm),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXs),
            itemVerticalAlignment = Alignment.CenterVertically,
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
                )
            }

            if (availableModes.size > 1) {
                ViewModeSelector(availableModes, modeEnabled, viewMode, onViewModeChange = { viewMode = it })
            }
            if (viewMode == ChartViewMode.Average) {
                LabeledSwitch("Show CI", checked = showCi, onCheckedChange = { showCi = it })
            }
            if (viewMode == ChartViewMode.Histogram) {
                HistogramControls(
                    numBins = numBins,
                    onNumBinsChange = { numBins = it },
                    showDensity = showDensity,
                    onShowDensityChange = { showDensity = it },
                    logScale = logScale,
                    onLogScaleChange = { logScale = it },
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingLg),
        ) {
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
                            HistogramChart(
                                metricByScenario = filteredMetrics,
                                simulations = simulations,
                                numBins = numBins,
                                showDensity = showDensity,
                                logScale = logScale,
                            )
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
