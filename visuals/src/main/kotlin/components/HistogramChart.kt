package components

import Dimensions
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
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.dynatrace.dynahist.Histogram
import com.group7.metrics.MetricGroup
import com.patrykandpatrick.vico.compose.cartesian.*
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import generateDistinctColors
import kotlin.math.*
import kotlinx.collections.immutable.ImmutableMap

internal data class HistogramBin(val lowerBound: Double, val upperBound: Double)

internal data class HistogramData(val bins: List<HistogramBin>, val frequenciesByScenario: Map<String, List<Int>>)

internal fun computeHistogram(histogramsByScenario: Map<String, Histogram>): HistogramData {
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

    val totalCount = nonEmpty.values.sumOf { it.totalCount }
    // Sturges' rule
    val binCount = ceil(log2(totalCount.toDouble()) + 1).toInt().coerceIn(3, 50)
    val binWidth = (globalMax - globalMin) / binCount

    val bins =
        (0 until binCount).map { i ->
            HistogramBin(lowerBound = globalMin + i * binWidth, upperBound = globalMin + (i + 1) * binWidth)
        }

    val frequenciesByScenario =
        histogramsByScenario.mapValues { (_, histogram) ->
            val counts = IntArray(binCount)
            for (bin in histogram.nonEmptyBinsAscending()) {
                val midpoint = (bin.lowerBound + bin.upperBound) / 2.0
                val index = ((midpoint - globalMin) / binWidth).toInt().coerceIn(0, binCount - 1)
                counts[index] += bin.binCount.toInt()
            }
            counts.toList()
        }

    return HistogramData(bins, frequenciesByScenario)
}

/** Compute a nice round maximum and tick step for integer frequency axes. Want a step in [1,2,5,10]. */
internal fun computeNiceMaxAndStep(maxValue: Int): Pair<Int, Int> {
    if (maxValue <= 0) return 1 to 1

    val targetStepCount = 10
    val roughStep = maxValue / targetStepCount.toDouble()
    val magnitude = 10.0.pow(floor(log10(roughStep.coerceAtLeast(1.0)))).toInt().coerceAtLeast(1)

    // Pick the nearest (linear distance) "nice" step from allowed values
    val niceSteps = listOf(1, 2, 5, 10)
    val normalisedRoughStep = roughStep / magnitude
    val step = (niceSteps.minByOrNull { abs(it - normalisedRoughStep) } ?: 1) * magnitude

    val niceMax = ceil(maxValue / step.toDouble()).toInt() * step
    return niceMax to step
}

@Composable
fun HistogramChart(metricByScenario: ImmutableMap<String, MetricGroup>, simulations: Map<String, MetricsPanelState>) {
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

    LaunchedEffect(metricByScenario, simulations.values.map { it.latestTimeSeen }) {
        val histogramsByScenario =
            metricByScenario
                .mapNotNull { (simName, metricGroup) ->
                    val histogram =
                        simulations.getValue(simName).getHistogram(metricGroup.raw) ?: return@mapNotNull null
                    simName to histogram
                }
                .toMap()

        if (histogramsByScenario.isEmpty()) {
            histogramData = null
            return@LaunchedEffect
        }
        val data = computeHistogram(histogramsByScenario)
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

            if (metricByScenario.size == 1) {
                SingleHistogramVico(data, scenarioColors, metricByScenario)
            } else {
                MultiHistogramCanvas(data, scenarioColors, metricByScenario)
            }
        } else {
            Text("No data to display")
        }
    }
}

/** Single-scenario histogram rendered with Vico ColumnCartesianLayer. */
@Composable
private fun SingleHistogramVico(
    data: HistogramData,
    scenarioColors: Map<String, Color>,
    metricByScenario: ImmutableMap<String, MetricGroup>,
) {
    val (simName, _) = metricByScenario.entries.single()
    val color = scenarioColors.getValue(simName)
    val modelProducer = remember { CartesianChartModelProducer() }

    val columnProvider =
        ColumnCartesianLayer.ColumnProvider.series(
            rememberLineComponent(
                fill = Fill(color),
                strokeFill = Fill.Black,
                strokeThickness = Dimensions.borderWidthThin,
            )
        )

    LaunchedEffect(data) {
        val frequencies = data.frequenciesByScenario[simName] ?: return@LaunchedEffect
        modelProducer.runTransaction {
            columnSeries {
                // x-axis is from 0->n so +0.5 centres it for that bin
                series(x = frequencies.indices.map { it.toDouble() + 0.5 }, y = frequencies.map { it as Number })
            }
        }
    }

    val binWidth = data.bins[0].upperBound - data.bins[0].lowerBound
    val globalMin = data.bins[0].lowerBound
    val boundaryFormatter =
        remember(data) { CartesianValueFormatter { _, value, _ -> "%.2f".format(globalMin + value * binWidth) } }

    // Vico's internal state uses rememberSaveable for animation/zoom tracking.
    // When the histogram data changes, Vico's remembered type can change and cause
    // ClassCastExceptions when restoring stale state. Setting the registry to null
    // prevents rememberSaveable from persisting anything inside this chart.
    CompositionLocalProvider(LocalSaveableStateRegistry provides null) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)) {
            val markerRecorder = remember { MarkerRecorder() }
            val layerBoundsCapture = remember { LayerBoundsCapture() }

            CartesianChartHost(
                chart =
                    rememberCartesianChart(
                        rememberColumnCartesianLayer(
                            columnProvider = columnProvider,
                            columnCollectionSpacing = 0.dp,
                            rangeProvider =
                                remember(data) {
                                    CartesianLayerRangeProvider.fixed(minX = 0.0, maxX = data.bins.size.toDouble())
                                },
                        ),
                        startAxis =
                            VerticalAxis.rememberStart(
                                label =
                                    rememberAxisLabelComponent(
                                        style = TextStyle(color = MaterialTheme.colorScheme.onSurface)
                                    )
                            ),
                        bottomAxis =
                            HorizontalAxis.rememberBottom(
                                label =
                                    rememberAxisLabelComponent(
                                        style = TextStyle(color = MaterialTheme.colorScheme.onSurface)
                                    ),
                                valueFormatter = boundaryFormatter,
                                itemPlacer = remember { HorizontalAxis.ItemPlacer.aligned() },
                            ),
                        markerController = markerRecorder,
                        marker = NoOpMarker,
                        decorations = listOf(layerBoundsCapture),
                    ),
                modelProducer = modelProducer,
                modifier = Modifier.matchParentSize().graphicsLayer(),
                zoomState =
                    rememberVicoZoomState(
                        initialZoom = Zoom.Content,
                        minZoom = Zoom.Content,
                        maxZoom = Zoom.Content,
                        zoomEnabled = false,
                    ),
                scrollState = rememberVicoScrollState(scrollEnabled = false),
            )

            markerRecorder.marker
                ?.takeIf { it.canvasY.toInt() in 0..constraints.maxHeight }
                ?.let { (x, _, canvasY) ->
                    val binIndex = (x - 0.5).toInt()
                    if (binIndex in data.bins.indices) {
                        val binCenterX = layerBoundsCapture.dataToPixelX(binIndex + 0.5f)
                        GuideLine(binCenterX, layerBoundsCapture.layerBounds, Modifier.matchParentSize())
                        DisplayNear(anchorX = binCenterX.toInt(), anchorY = canvasY.toInt()) {
                            HistogramTooltip(
                                data.bins[binIndex],
                                data.frequenciesByScenario,
                                binIndex,
                                scenarioColors,
                                metricByScenario.keys,
                            )
                        }
                    }
                }
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
    niceMax: Int,
    tickStep: Int,
    binCount: Int,
    color: Color,
    dash: PathEffect,
) {
    val stroke = Dimensions.borderWidth.toPx()
    var gridTick = tickStep
    while (gridTick <= niceMax) {
        val y = geo.chartBottom - (gridTick.toFloat() / niceMax) * geo.chartHeight
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
    scenarios: Set<String>,
    colors: Map<String, Color>,
    niceMax: Int,
) {
    for (binIndex in data.bins.indices) {
        scenarios
            .map { it to (data.frequenciesByScenario[it]?.get(binIndex) ?: 0) }
            .sortedByDescending { it.second }
            .forEach { (simName, freq) ->
                if (freq <= 0) return@forEach
                val barHeight = (freq.toFloat() / niceMax) * geo.chartHeight
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
    niceMax: Int,
    tickStep: Int,
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
    var tick = 0
    while (tick <= niceMax) {
        val y = geo.chartBottom - (tick.toFloat() / niceMax) * geo.chartHeight
        drawLine(axisColor, Offset(geo.chartLeft - tickPx, y), Offset(geo.chartLeft, y))
        val result = textMeasurer.measure(tick.toString(), labelStyle)
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
    val firstWidth = textMeasurer.measure("%.2f".format(data.bins.first().lowerBound), labelStyle).size.width
    val lastWidth = textMeasurer.measure("%.2f".format(data.bins.last().upperBound), labelStyle).size.width
    return maxOf(firstWidth, lastWidth) + Dimensions.spacingXs.toPx() * 2
}

/**
 * X-axis line with centred bin-boundary labels. Label count is capped to avoid overlap, using the actual measured width
 * of the widest boundary label plus a small margin as the minimum spacing.
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
        val result = textMeasurer.measure("%.2f".format(value), labelStyle)
        drawText(result, topLeft = Offset(x - result.size.width / 2f, geo.chartBottom + tickPx + gapPx))
    }
}

/** Multi-scenario histogram rendered with custom Canvas (per-bin draw ordering). */
@Composable
private fun MultiHistogramCanvas(
    data: HistogramData,
    scenarioColors: Map<String, Color>,
    metricByScenario: ImmutableMap<String, MetricGroup>,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = axisColor)

    val maxFreq = data.frequenciesByScenario.values.maxOfOrNull { freqs -> freqs.maxOrNull() ?: 0 } ?: 0
    val (niceMax, tickStep) = remember(maxFreq) { computeNiceMaxAndStep(maxFreq) }
    // for ints width of widest Y label = width of max label
    val yLabelWidth =
        remember(niceMax, textMeasurer, labelStyle) {
            textMeasurer.measure(niceMax.toString(), labelStyle).size.width.toFloat()
        }
    val leftPaddingPx = yLabelWidth + with(density) { 8.dp.toPx() }
    val bottomPaddingPx = with(density) { 32.dp.toPx() }
    val rightLabelWidth =
        remember(data, textMeasurer, labelStyle) {
            textMeasurer.measure("%.2f".format(data.bins.last().upperBound), labelStyle).size.width.toFloat()
        }
    // half the width of the last X label + 4dp gap
    val rightPaddingPx = rightLabelWidth / 2f + with(density) { 4.dp.toPx() }

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), phase = 0f) }
    var hoverBinIndex by remember { mutableStateOf(-1) }
    var hoverOffset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(bottom = Dimensions.spacingMd)) {
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
            drawHistogramBars(geo, data, metricByScenario.keys, scenarioColors, niceMax)
            drawHistogramYAxis(geo, niceMax, tickStep, axisColor, textMeasurer, labelStyle)
            drawHistogramXAxis(geo, data, axisColor, textMeasurer, labelStyle)

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
) {
    val entries =
        scenarios
            .map { simName -> simName to (frequenciesByScenario[simName]?.getOrNull(binIndex) ?: 0) }
            .sortedByDescending { it.second }
    val total = entries.sumOf { it.second }

    Column(modifier = Modifier.background(Color.Black, shape = RoundedCornerShape(8.dp)).padding(8.dp)) {
        Text(
            text = "${"%.2f".format(bin.lowerBound)} – ${"%.2f".format(bin.upperBound)}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
        )
        for ((simName, count) in entries) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier.size(Dimensions.spacingSm)
                            .background(scenarioColors.getValue(simName), shape = CircleShape)
                )
                Text(text = "$simName: $count", style = MaterialTheme.typography.labelSmall, color = Color.White)
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
