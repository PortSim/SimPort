package components

import DefaultColorPalette
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.group7.Simulator
import com.group7.metrics.ContinuousMetric
import com.group7.metrics.Metric
import com.group7.metrics.MetricGroup
import com.patrykandpatrick.vico.compose.cartesian.*
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.*
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.ImmutableMap

@Composable
fun SummaryChart(
    metricByScenario: ImmutableMap<String, MetricGroup>,
    simulations: Map<String, MetricsPanelState>,
    showRaw: Boolean,
) {
    val scenarioColors =
        remember(simulations) {
            simulations.keys
                .asSequence()
                .mapIndexed { index, simName ->
                    simName to DefaultColorPalette.chartColors[index % DefaultColorPalette.chartColors.size]
                }
                .toMap()
        }

    val modelProducer = remember { CartesianChartModelProducer() }
    val legendItems =
        remember(metricByScenario) {
            metricByScenario.map { (simName, metricGroup) ->
                "$simName - ${metricGroup.name}" to scenarioColors.getValue(simName)
            }
        }
    val lines =
        remember(metricByScenario, showRaw) {
            LineCartesianLayer.LineProvider.series(
                metricByScenario.flatMap { (simName, metricGroup) ->
                    val baseColor = scenarioColors.getValue(simName)

                    if (showRaw) {
                        listOf(metricLine(metricGroup.raw, baseColor, LineCartesianLayer.LineStroke.Continuous()))
                    } else {
                        listOfNotNull(
                            metricGroup.moments?.mean?.let {
                                metricLine(it, baseColor, LineCartesianLayer.LineStroke.Continuous())
                            },
                            metricGroup.moments?.lowerCi?.let {
                                metricLine(it, baseColor.copy(alpha = 0.6f), LineCartesianLayer.LineStroke.Dashed())
                            },
                            metricGroup.moments?.upperCi?.let {
                                metricLine(it, baseColor.copy(alpha = 0.6f), LineCartesianLayer.LineStroke.Dashed())
                            },
                        )
                    }
                }
            )
        }

    LaunchedEffect(metricByScenario, showRaw, simulations.values.map { it.latestTimeSeen }) {
        val actions = mutableListOf<LineCartesianLayerModel.BuilderScope.() -> Unit>()
        for ((simName, metricGroup) in metricByScenario) {
            val metricsState = simulations.getValue(simName)

            val metrics =
                if (showRaw) {
                    listOf(metricGroup.raw)
                } else {
                    listOfNotNull(metricGroup.moments?.mean, metricGroup.moments?.lowerCi, metricGroup.moments?.upperCi)
                }

            for (metric in metrics) {
                val (x, y) = metricsState.getMetricData(metric).unzip()
                if (x.isNotEmpty()) {
                    actions.add { series(x = x.map { (it - Simulator.START_TIME).inWholeSeconds }, y = y) }
                }
            }
        }
        if (actions.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    for (action in actions) {
                        action()
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White, shape = RoundedCornerShape(8.dp)).padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChartLegend(items = legendItems)
        }

        CartesianChartHost(
            chart =
                rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lines,
                        rangeProvider = remember { CartesianLayerRangeProvider.fixed(minX = 0.0) },
                    ),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = InstantFormatter),
                    marker =
                        rememberDefaultCartesianMarker(
                            rememberTextComponent(),
                            labelPosition = DefaultCartesianMarker.LabelPosition.Top,
                            guideline = rememberLineComponent(),
                        ),
                ),
            modelProducer = modelProducer,
            modifier = Modifier.fillMaxSize().padding(bottom = 16.dp).graphicsLayer(),
            zoomState = rememberVicoZoomState(initialZoom = Zoom.Content),
            scrollState = rememberVicoScrollState(scrollEnabled = false),
        )
    }
}

private object InstantFormatter : CartesianValueFormatter {
    override fun format(
        context: CartesianMeasuringContext,
        value: Double,
        verticalAxisPosition: Axis.Position.Vertical?,
    ) = value.seconds.toString()
}

private fun metricLine(metric: Metric, color: Color, stroke: LineCartesianLayer.LineStroke) =
    if (metric is ContinuousMetric) {
        LineCartesianLayer.Line(fill = LineCartesianLayer.LineFill.single(Fill(color)), stroke = stroke)
    } else {
        LineCartesianLayer.Line(
            fill = LineCartesianLayer.LineFill.single(Fill(Color.Transparent)),
            pointProvider =
                LineCartesianLayer.PointProvider.single(
                    LineCartesianLayer.Point(ShapeComponent(fill = Fill(color), shape = CircleShape))
                ),
        )
    }
