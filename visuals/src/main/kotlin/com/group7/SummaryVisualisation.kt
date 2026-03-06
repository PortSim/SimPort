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
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentSet

private val EmptyStateHeight = 300.dp

/** Builds a three-level index: metric name → node label → simulation name → MetricGroup. */
internal fun buildMetricIndex(
    simulations: ImmutableMap<String, SimulationState>
): Map<String, Map<String?, Map<String, MetricGroup>>> {
    val result = mutableMapOf<String, MutableMap<String?, MutableMap<String, MetricGroup>>>()

    for ((simulationName, metricsState) in simulations) {
        val nodeNames = assignNodeNames(metricsState.scenario)

        for ((metricName, metricGroups) in metricsState.metricGroups) {
            for (metricGroup in metricGroups) {
                result
                    .getOrPut(metricName) { sortedMapOf(nullsFirst()) }
                    .getOrPut(metricGroup.associatedNode?.let(nodeNames::getValue), ::sortedMapOf)[simulationName] =
                    metricGroup
            }
        }
    }

    return result
}

/** Bundled chart-option state so the main composable doesn't need five separate [mutableStateOf] declarations. */
private class ChartOptions {
    var viewMode by mutableStateOf(ChartViewMode.Average)
    var showCi by mutableStateOf(true)
    var numBins by mutableStateOf<Int?>(null) // null = auto (Sturges' rule)
    var showDensity by mutableStateOf(false)
    var logScale by mutableStateOf(false)
}

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
            // Determine why a mode is disabled and show appropriate tooltip
            val tooltip =
                when {
                    enabled -> null
                    mode == ChartViewMode.Histogram -> "Sample never triggered"
                    mode == ChartViewMode.Average -> "Convergence not yet reached"
                    else -> null
                }
            val button =
                @Composable {
                    // Segmented button with proper shape (rounded corners on ends, straight in middle)
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
            // Wrap button in tooltip if there's a reason it's disabled
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
    // Bin count control with slider and auto-reset button
    Row(modifier = Modifier.weight(1f).widthIn(min = 250.dp), verticalAlignment = Alignment.CenterVertically) {
        // Slider for adjusting bin count (3-100), uses 25 as visual default for "Auto"
        LabeledSlider(
            value = (numBins ?: 25).toFloat(),
            onValueChange = { onNumBinsChange(it.roundToInt()) },
            valueRange = 3f..100f,
            steps = 96,
            minLabel = "3",
            maxLabel = "100",
            valueLabel = "Bins: ${numBins ?: "Auto"}",
            modifier = Modifier.weight(1f),
        )
        // Button to reset to auto bin calculation (only visible when manually set)
        TextButton(
            onClick = { onNumBinsChange(null) },
            enabled = numBins != null,
            modifier = Modifier.alpha(if (numBins != null) 1f else 0f),
        ) {
            Text("Auto")
        }
    }
    // Toggles for histogram display options
    Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSm)) {
        // Toggle between count and probability density display
        LabeledSwitch("Probability", checked = showDensity, onCheckedChange = onShowDensityChange)
        // Toggle logarithmic X-axis scale
        LabeledSwitch("Log X", checked = logScale, onCheckedChange = onLogScaleChange)
    }
}

/** Toolbar: metric/node/scenario dropdowns, view mode selector, and mode-specific controls. */
@Composable
private fun SummaryToolbar(
    metricIndex: Map<String, Map<String?, Map<String, MetricGroup>>>,
    selectedMetric: String,
    onMetricChange: (String) -> Unit,
    selectedNodeLabel: String?,
    onNodeChange: (String?) -> Unit,
    validScenarios: PersistentSet<String>,
    effectiveScenarios: PersistentSet<String>,
    onScenariosChange: (PersistentSet<String>) -> Unit,
    availableModes: List<ChartViewMode>,
    modeEnabled: Map<ChartViewMode, Boolean>,
    options: ChartOptions,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSm),
        verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXs),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Dropdown(
            options = metricIndex.keys,
            selected = selectedMetric,
            onSelected = onMetricChange,
            label = { Text("Metric") },
        )

        Dropdown(
            options = metricIndex.getValue(selectedMetric).keys,
            selected = selectedNodeLabel,
            onSelected = onNodeChange,
            label = { Text("Node") },
            displayText = { it ?: GLOBAL_NODE_LABEL },
        )

        if (validScenarios.size >= 2) {
            MultiSelectDropdown(
                label = "Scenarios",
                options = validScenarios.sorted(),
                selectedOptions = effectiveScenarios,
                onSelectionChange = onScenariosChange,
            )
        }

        if (availableModes.size > 1) {
            ViewModeSelector(
                availableModes,
                modeEnabled,
                options.viewMode,
                onViewModeChange = { options.viewMode = it },
            )
        }
        if (options.viewMode == ChartViewMode.Average) {
            LabeledSwitch("Show CI", checked = options.showCi, onCheckedChange = { options.showCi = it })
        }
        if (options.viewMode == ChartViewMode.Histogram) {
            HistogramControls(
                numBins = options.numBins,
                onNumBinsChange = { options.numBins = it },
                showDensity = options.showDensity,
                onShowDensityChange = { options.showDensity = it },
                logScale = options.logScale,
                onLogScaleChange = { options.logScale = it },
            )
        }
    }
}

/** Chart area: dispatches to Raw / Average / Histogram based on the current view mode. */
@Composable
private fun ColumnScope.SummaryChartArea(
    filteredMetrics: ImmutableMap<String, MetricGroup>,
    simulations: ImmutableMap<String, SimulationState>,
    options: ChartOptions,
) {
    Column(
        modifier = Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(Dimensions.spacingLg),
    ) {
        // key(viewMode) tears down and recreates all composition state on view mode switch,
        // so Vico's internal rememberSaveable state (zoom, animation) is discarded and
        // cannot be erroneously restored into the wrong type (e.g. SpringSpec → SaveableHolder).
        key(options.viewMode) {
            when (options.viewMode) {
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
                        showCi = options.showCi,
                    )
                ChartViewMode.Histogram ->
                    HistogramChart(
                        metricByScenario = filteredMetrics,
                        simulations = simulations,
                        numBins = options.numBins,
                        showDensity = options.showDensity,
                        logScale = options.logScale,
                    )
            }
        }
    }
}

/**
 * Aggregated metrics view for one or more simulations.
 *
 * Builds a three-level index (metric → node → simulation) from the simulation data and lets the user select a metric,
 * node, and scenario subset. Supports three chart modes: Raw time-series, Average (with optional CI bands), and
 * Histogram (with configurable bins and density scaling).
 */
@Composable
fun SummaryVisualisation(simulations: ImmutableMap<String, SimulationState>) {
    if (simulations.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No simulations to display") }
        return
    }

    val metricIndex = remember(simulations) { buildMetricIndex(simulations) }

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

    val options = remember { ChartOptions() }

    // Coerce to a valid enabled mode when available modes change
    LaunchedEffect(availableModes, modeEnabled) {
        if (options.viewMode !in availableModes || modeEnabled[options.viewMode] == false) {
            options.viewMode = availableModes.firstOrNull { modeEnabled[it] != false } ?: ChartViewMode.Raw
        }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow).padding(Dimensions.spacingMd),
        verticalArrangement = Arrangement.spacedBy(Dimensions.spacingSm),
    ) {
        SummaryToolbar(
            metricIndex = metricIndex,
            selectedMetric = selectedMetric,
            onMetricChange = { selectedMetric = it },
            selectedNodeLabel = selectedNodeLabel,
            onNodeChange = { selectedNodeLabel = it },
            validScenarios = validScenarios,
            effectiveScenarios = effectiveScenarios,
            onScenariosChange = { selectedScenarios = it },
            availableModes = availableModes,
            modeEnabled = modeEnabled,
            options = options,
        )

        if (effectiveScenarios.isNotEmpty()) {
            val filteredMetrics =
                metricIndex
                    .getValue(selectedMetric)
                    .getValue(selectedNodeLabel)
                    .filterKeys { it in effectiveScenarios }
                    .toImmutableMap()

            SummaryChartArea(filteredMetrics, simulations, options)
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(EmptyStateHeight), contentAlignment = Alignment.Center) {
                Text("Select at least one scenario to display chart")
            }
        }
    }
}
