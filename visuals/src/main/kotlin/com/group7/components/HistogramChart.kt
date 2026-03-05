package com.group7.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.dynatrace.dynahist.Histogram
import com.group7.Dimensions
import com.group7.generateDistinctColors
import com.group7.metrics.MetricGroup
import com.group7.state.SimulationState
import kotlin.math.*
import kotlinx.collections.immutable.ImmutableMap

internal data class HistogramBin(val lowerBound: Double, val upperBound: Double)

internal data class HistogramData(
    val bins: List<HistogramBin>,
    val frequenciesByScenario: Map<String, List<Int>>,
    val logScale: Boolean = false,
) {
    fun formatBoundary(value: Double): String =
        if (logScale && value > 0) "%.2f".format(log10(value)) else "%.2f".format(value)
}

internal fun computeHistogram(
    histogramsByScenario: Map<String, Histogram>,
    requestedBinCount: Int? = null,
    logScale: Boolean = false,
): HistogramData {
    val nonEmpty = histogramsByScenario.filterValues { !it.isEmpty }
    if (nonEmpty.isEmpty()) return HistogramData(emptyList(), emptyMap())

    // dynahist stores the exact min/max of added values — no edge-case workarounds needed.
    val globalMin = nonEmpty.values.minOf { it.min }
    val globalMax = nonEmpty.values.maxOf { it.max }

    if (globalMax - globalMin < 1e-10) {
        // All values are effectively the same — create a single bin.
        val halfRange = maxOf(0.01, abs(globalMin) * 0.05)
        val bin = HistogramBin(globalMin, globalMin + halfRange)
        return HistogramData(
            bins = listOf(bin),
            frequenciesByScenario = histogramsByScenario.mapValues { (_, h) -> listOf(h.totalCount.toInt()) },
        )
    }

    val binCount =
        if (requestedBinCount != null) {
            requestedBinCount.coerceIn(3, 100)
        } else {
            val totalCount = nonEmpty.values.sumOf { it.totalCount }
            // Sturges' rule
            ceil(log2(totalCount.toDouble()) + 1).toInt().coerceIn(3, 100)
        }

    // Work in log10 space when logScale is enabled and all values are positive; otherwise linear.
    val useLog = logScale && globalMin > 0
    val transform: (Double) -> Double = if (useLog) ::log10 else { x -> x }
    val untransform: (Double) -> Double = if (useLog) { x -> 10.0.pow(x) } else { x -> x }

    val effMin = transform(globalMin)
    val effMax = transform(globalMax)
    val binWidth = (effMax - effMin) / binCount

    val bins =
        (0 until binCount).map { i ->
            HistogramBin(
                lowerBound = untransform(effMin + i * binWidth),
                upperBound = untransform(effMin + (i + 1) * binWidth),
            )
        }

    val frequenciesByScenario =
        histogramsByScenario.mapValues { (_, histogram) ->
            val counts = IntArray(binCount)
            for (bin in histogram.nonEmptyBinsAscending()) {
                val midpoint = (bin.lowerBound + bin.upperBound) / 2.0
                val index = ((transform(midpoint) - effMin) / binWidth).toInt().coerceIn(0, binCount - 1)
                counts[index] += bin.binCount.toInt()
            }
            counts.toList()
        }

    return HistogramData(bins, frequenciesByScenario, logScale)
}

/** Compute a nice round maximum and tick step for Double-valued axes (used for density mode). */
internal fun computeNiceMaxAndStepDouble(maxValue: Double): Pair<Double, Double> {
    if (maxValue <= 0.0) return 1.0 to 1.0

    val targetStepCount = 10
    val roughStep = maxValue / targetStepCount
    val magnitude = 10.0.pow(floor(log10(roughStep.coerceAtLeast(1e-10))))

    val niceSteps = listOf(1.0, 2.0, 5.0, 10.0)
    val normalisedRoughStep = roughStep / magnitude
    val step = (niceSteps.minBy { abs(it - normalisedRoughStep) }) * magnitude

    val niceMax = ceil(maxValue / step) * step
    return niceMax to step
}

/**
 * Converts raw integer frequency counts to display values, optionally applying density normalization: density = count /
 * totalCount (relative frequency). When multiple normalises each based on their individual totalCount not
 * globalTotalCount.
 */
internal fun computeDisplayValues(data: HistogramData, showDensity: Boolean): Map<String, List<Double>> {
    if (!showDensity) {
        return data.frequenciesByScenario.mapValues { (_, freqs) -> freqs.map { it.toDouble() } }
    }
    if (data.bins.isEmpty()) return data.frequenciesByScenario.mapValues { emptyList() }
    return data.frequenciesByScenario.mapValues { (_, freqs) ->
        val totalCount = freqs.sum()
        if (totalCount == 0) freqs.map { 0.0 } else freqs.map { count -> count.toDouble() / totalCount }
    }
}

/** Adaptive formatting for density values — avoids showing 0.0000 for small numbers. */
private fun formatDensityValue(value: Double): String =
    when {
        value == 0.0 -> "0"
        value >= 0.01 -> "%.3f".format(value)
        // 3 s.f. scientific notation
        else -> "%.3g".format(value)
    }

@Composable
fun HistogramChart(
    metricByScenario: ImmutableMap<String, MetricGroup>,
    simulations: Map<String, SimulationState>,
    numBins: Int? = null,
    showDensity: Boolean = false,
    logScale: Boolean = false,
) {
    val scenarioColors =
        remember(metricByScenario) {
            val colors = generateDistinctColors(metricByScenario.size)
            metricByScenario.keys.zip(colors).toMap()
        }
    val legendItems =
        remember(metricByScenario) {
            metricByScenario.map { (simName, metricGroup) ->
                "$simName - ${metricGroup.name}" to scenarioColors.getValue(simName)
            }
        }

    var histogramData by remember { mutableStateOf<HistogramData?>(null) }

    LaunchedEffect(metricByScenario, numBins, logScale, simulations.values.map { it.histogramsState.latestTimeSeen }) {
        val histogramsByScenario =
            metricByScenario
                .mapNotNull { (simName, metricGroup) ->
                    val histogram =
                        metricGroup.raw?.let { simulations.getValue(simName).histogramsState.getHistogram(it) }
                            ?: return@mapNotNull null
                    simName to histogram
                }
                .toMap()

        if (histogramsByScenario.isEmpty()) {
            histogramData = null
            return@LaunchedEffect
        }
        val data = computeHistogram(histogramsByScenario, numBins, logScale)
        histogramData = if (data.bins.isEmpty()) null else data
    }

    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val data = histogramData
        if (data != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChartLegend(items = legendItems)
            }

            HistogramCanvas(data, scenarioColors, metricByScenario, showDensity)
        } else {
            Text("No data to display")
        }
    }
}

/** Layout parameters for the chart drawing area, computed once and shared across all draw passes. */
private data class ChartGeometry(
    val chartLeft: Float,
    val chartBottom: Float,
    val chartWidth: Float,
    val chartHeight: Float,
    val binPixelWidth: Float,
)

/** Dashed horizontal lines at each Y tick and dashed vertical lines at each bin boundary. */
private fun DrawScope.drawHistogramGrid(
    geo: ChartGeometry,
    niceMax: Double,
    tickStep: Double,
    binCount: Int,
    color: Color,
    dash: PathEffect,
) {
    val stroke = Dimensions.borderWidth.toPx()
    var gridTick = tickStep
    // 0.01 * tickStep is a guard against floating point addition errors
    while (gridTick <= niceMax + tickStep * 0.01) {
        val y = geo.chartBottom - (gridTick / niceMax).toFloat() * geo.chartHeight
        drawLine(
            color,
            Offset(geo.chartLeft, y),
            Offset(geo.chartLeft + geo.chartWidth, y),
            strokeWidth = stroke,
            pathEffect = dash,
        )
        gridTick += tickStep
    }
    for (i in 1 until binCount) {
        val x = geo.chartLeft + i * geo.binPixelWidth
        drawLine(color, Offset(x, 0f), Offset(x, geo.chartBottom), strokeWidth = stroke, pathEffect = dash)
    }
}

/**
 * Bars with per-bin z-ordering: within each bin, the tallest scenario is drawn first (furthest back) so shorter bars
 * always appear in front — overlay semantics, not stacked.
 */
private fun DrawScope.drawHistogramBars(
    geo: ChartGeometry,
    data: HistogramData,
    displayValues: Map<String, List<Double>>,
    scenarios: Set<String>,
    colors: Map<String, Color>,
    niceMax: Double,
) {
    for (binIndex in data.bins.indices) {
        scenarios
            .map { it to (displayValues[it]?.get(binIndex) ?: 0.0) }
            .sortedByDescending { it.second }
            .forEach { (simName, value) ->
                if (value <= 0.0) return@forEach
                val barHeight = (value / niceMax).toFloat().coerceIn(0f, 1f) * geo.chartHeight
                val topLeft = Offset(geo.chartLeft + binIndex * geo.binPixelWidth, geo.chartBottom - barHeight)
                val size = Size(geo.binPixelWidth, barHeight)
                drawRect(color = colors.getValue(simName), topLeft = topLeft, size = size)
                drawRect(
                    color = Color.Black,
                    topLeft = topLeft,
                    size = size,
                    style = Stroke(width = Dimensions.borderWidthThin.toPx()),
                )
            }
    }
}

/** Y-axis line with evenly spaced tick marks and right-aligned numeric labels. */
private fun DrawScope.drawHistogramYAxis(
    geo: ChartGeometry,
    niceMax: Double,
    tickStep: Double,
    showDensity: Boolean,
    axisColor: Color,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
) {
    val tickPx = Dimensions.axisTickLength.toPx()
    val gapPx = Dimensions.axisLabelGap.toPx()
    drawLine(
        axisColor,
        Offset(geo.chartLeft, 0f),
        Offset(geo.chartLeft, geo.chartBottom),
        strokeWidth = Dimensions.borderWidth.toPx(),
    )
    var tick = 0.0
    while (tick <= niceMax + tickStep * 0.01) {
        val y = geo.chartBottom - (tick / niceMax).toFloat() * geo.chartHeight
        drawLine(axisColor, Offset(geo.chartLeft - tickPx, y), Offset(geo.chartLeft, y))
        val label = if (showDensity) formatDensityValue(tick) else "%.0f".format(tick)
        val result = textMeasurer.measure(label, labelStyle)
        drawText(
            result,
            topLeft = Offset(geo.chartLeft - tickPx - gapPx - result.size.width, y - result.size.height / 2f),
        )
        tick += tickStep
    }
}

/**
 * Measures the first and last boundary labels and returns the minimum pixel spacing per label (widest + side margins).
 */
private fun DrawScope.xAxisLabelMinSpacing(
    data: HistogramData,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
): Float {
    val firstWidth = textMeasurer.measure(data.formatBoundary(data.bins.first().lowerBound), labelStyle).size.width
    val lastWidth = textMeasurer.measure(data.formatBoundary(data.bins.last().upperBound), labelStyle).size.width
    return maxOf(firstWidth, lastWidth) + Dimensions.spacingXs.toPx() * 2
}

/**
 * X-axis line with centred bin-boundary labels. Label count is capped to avoid overlap, using the actual measured width
 * of the widest boundary label plus a small margin as the minimum spacing. In log mode, labels show log10 of the
 * boundary value.
 */
private fun DrawScope.drawHistogramXAxis(
    geo: ChartGeometry,
    data: HistogramData,
    axisColor: Color,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
) {
    val tickPx = Dimensions.axisTickLength.toPx()
    val gapPx = Dimensions.axisLabelGap.toPx()
    drawLine(
        axisColor,
        Offset(geo.chartLeft, geo.chartBottom),
        Offset(geo.chartLeft + geo.chartWidth, geo.chartBottom),
        strokeWidth = Dimensions.borderWidth.toPx(),
    )
    val maxXLabels = max(2, (geo.chartWidth / xAxisLabelMinSpacing(data, textMeasurer, labelStyle)).toInt())
    val labelInterval = max(1, (data.bins.size + maxXLabels - 1) / maxXLabels)
    for (i in 0..data.bins.size step labelInterval) {
        val x = geo.chartLeft + i * geo.binPixelWidth
        drawLine(axisColor, Offset(x, geo.chartBottom), Offset(x, geo.chartBottom + tickPx))
        val value = if (i < data.bins.size) data.bins[i].lowerBound else data.bins.last().upperBound
        val result = textMeasurer.measure(data.formatBoundary(value), labelStyle)
        drawText(result, topLeft = Offset(x - result.size.width / 2f, geo.chartBottom + tickPx + gapPx))
    }
}

@Composable
private fun HistogramCanvas(
    data: HistogramData,
    scenarioColors: Map<String, Color>,
    metricByScenario: ImmutableMap<String, MetricGroup>,
    showDensity: Boolean,
) {
    val metricName = metricByScenario.values.first().name
    val xAxisTitle = if (data.logScale) "log\u2081\u2080( $metricName )" else metricName

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = axisColor)

    val displayValues = remember(data, showDensity) { computeDisplayValues(data, showDensity) }
    val maxDisplayValue = displayValues.values.maxOfOrNull { freqs -> freqs.maxOrNull() ?: 0.0 } ?: 0.0

    val (niceMax, tickStep) = remember(maxDisplayValue) { computeNiceMaxAndStepDouble(maxDisplayValue) }

    val yLabelWidth =
        remember(niceMax, showDensity, textMeasurer, labelStyle) {
            val label = if (showDensity) formatDensityValue(niceMax) else "%.0f".format(niceMax)
            textMeasurer.measure(label, labelStyle).size.width.toFloat()
        }
    val leftPaddingPx = yLabelWidth + with(density) { 8.dp.toPx() }
    // Measure actual rendered text to get line height (labelStyle.lineHeight may be Unspecified)
    val textLineHeight = textMeasurer.measure(xAxisTitle, labelStyle).size.height.toFloat()
    // tick + gap + 2 text lines (x-axis boundary labels + x-axis title) + gap between them
    val bottomPaddingPx =
        with(density) {
            Dimensions.axisTickLength.toPx() +
                Dimensions.axisLabelGap.toPx() +
                2 * textLineHeight +
                Dimensions.spacingXs.toPx()
        }
    val rightLabelWidth =
        remember(data, textMeasurer, labelStyle) {
            val label = data.formatBoundary(data.bins.last().upperBound)
            textMeasurer.measure(label, labelStyle).size.width.toFloat()
        }
    // half the width of the last X label + 4dp gap
    val rightPaddingPx = rightLabelWidth / 2f + with(density) { 4.dp.toPx() }

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), phase = 0f) }
    var hoverBinIndex by remember { mutableStateOf(-1) }
    var hoverOffset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // DisplayNear is a composable, so binCenterX must be computed during composition.
        // size.width (Canvas draw scope) is only available during the draw phase (too late).
        // BoxWithConstraints exposes constraints.maxWidth during composition; since the Canvas
        // fills this box, constraints.maxWidth == size.width, so the geometry is identical.
        val compositionChartWidth = constraints.maxWidth.toFloat() - leftPaddingPx - rightPaddingPx
        val compositionBinPixelWidth = if (data.bins.isNotEmpty()) compositionChartWidth / data.bins.size else 0f
        val binCenterX =
            if (hoverBinIndex in data.bins.indices) leftPaddingPx + (hoverBinIndex + 0.5f) * compositionBinPixelWidth
            else 0f

        Canvas(
            Modifier.fillMaxSize().pointerInput(data) {
                val chartWidth = size.width - leftPaddingPx - rightPaddingPx
                val binPixelWidth = chartWidth / data.bins.size

                fun binAt(offset: Offset): Int {
                    val x = offset.x - leftPaddingPx
                    if (x !in 0.0f..chartWidth) return -1
                    return (x / binPixelWidth).toInt().coerceAtMost(data.bins.size - 1)
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    hoverBinIndex = binAt(down.position)
                    hoverOffset = down.position
                    try {
                        do {
                            val event = awaitPointerEvent()
                            val pos = event.changes.firstOrNull()?.position ?: break
                            hoverBinIndex = binAt(pos)
                            hoverOffset = pos
                        } while (event.changes.any { it.pressed })
                    } finally {
                        hoverBinIndex = -1
                    }
                }
            }
        ) {
            val geo =
                ChartGeometry(
                    chartLeft = leftPaddingPx,
                    chartBottom = size.height - bottomPaddingPx,
                    chartWidth = size.width - leftPaddingPx - rightPaddingPx,
                    chartHeight = size.height - bottomPaddingPx,
                    binPixelWidth = (size.width - leftPaddingPx - rightPaddingPx) / data.bins.size,
                )

            drawHistogramGrid(geo, niceMax, tickStep, data.bins.size, Color.Gray, dashEffect)
            drawHistogramBars(geo, data, displayValues, metricByScenario.keys, scenarioColors, niceMax)
            drawHistogramYAxis(geo, niceMax, tickStep, showDensity, axisColor, textMeasurer, labelStyle)
            drawHistogramXAxis(geo, data, axisColor, textMeasurer, labelStyle)

            // X-axis title centred below the axis labels
            val titleResult = textMeasurer.measure(xAxisTitle, labelStyle)
            val titleX = geo.chartLeft + (geo.chartWidth - titleResult.size.width) / 2f
            drawText(titleResult, topLeft = Offset(titleX, size.height - titleResult.size.height))

            if (hoverBinIndex in data.bins.indices) {
                val snapX = geo.chartLeft + (hoverBinIndex + 0.5f) * geo.binPixelWidth
                drawLine(
                    Color.Gray,
                    Offset(snapX, geo.chartBottom),
                    Offset(snapX, 0f),
                    strokeWidth = Dimensions.strokeWidth.toPx(),
                    pathEffect = dashEffect,
                )
            }
        }

        if (hoverBinIndex in data.bins.indices) {
            DisplayNear(anchorX = binCenterX.toInt(), anchorY = hoverOffset.y.toInt()) {
                HistogramTooltip(
                    data.bins[hoverBinIndex],
                    data.frequenciesByScenario,
                    hoverBinIndex,
                    scenarioColors,
                    metricByScenario.keys,
                    densityValues = if (showDensity) displayValues else null,
                )
            }
        }
    }
}

@Composable
internal fun HistogramTooltip(
    bin: HistogramBin,
    frequenciesByScenario: Map<String, List<Int>>,
    binIndex: Int,
    scenarioColors: Map<String, Color>,
    scenarios: Set<String>,
    densityValues: Map<String, List<Double>>? = null,
) {
    val entries =
        scenarios
            .map { simName ->
                Triple(
                    simName,
                    frequenciesByScenario[simName]?.getOrNull(binIndex) ?: 0,
                    densityValues?.get(simName)?.getOrNull(binIndex),
                )
            }
            .sortedByDescending { it.third ?: it.second.toDouble() }
    val total = entries.sumOf { it.second }

    Column(modifier = Modifier.background(Color.Black, shape = RoundedCornerShape(8.dp)).padding(8.dp)) {
        Text(
            text = "${"%.2f".format(bin.lowerBound)} – ${"%.2f".format(bin.upperBound)}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
        )
        for ((simName, count, density) in entries) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier.size(Dimensions.spacingSm)
                            .background(scenarioColors.getValue(simName), shape = CircleShape)
                )
                val text =
                    if (density != null) {
                        "$simName: $count (density: ${formatDensityValue(density)})"
                    } else {
                        "$simName: $count"
                    }
                Text(text = text, style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }
        if (entries.size > 1) {
            Text(
                text = "Total: $total",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}
