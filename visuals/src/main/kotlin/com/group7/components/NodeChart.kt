package com.group7.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.group7.Dimensions
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.Interaction

@Composable
fun ChartLegend(items: List<Pair<String, Color>>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingMd),
        verticalArrangement = Arrangement.Center,
    ) {
        items.forEach { (label, color) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(Dimensions.spacingSm).background(color, shape = CircleShape))
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

internal data class MarkerPosition(val x: Double, val canvasX: Float, val canvasY: Float)

/** A non-null [CartesianMarker] that draws nothing, required to enable chart interactions. */
internal object NoOpMarker : CartesianMarker

internal class MarkerRecorder : CartesianMarkerController {
    private var isMouseDown = false
    var marker by mutableStateOf<MarkerPosition?>(null)
        private set

    override fun shouldAcceptInteraction(interaction: Interaction, targets: List<CartesianMarker.Target>): Boolean {
        processInteraction(interaction, targets)
        return false
    }

    override fun shouldShowMarker(interaction: Interaction, targets: List<CartesianMarker.Target>): Boolean {
        return false
    }

    private fun processInteraction(interaction: Interaction, targets: List<CartesianMarker.Target>) {
        val target =
            targets.singleOrNull()
                ?: run {
                    marker = null
                    return
                }
        val newMarker = MarkerPosition(target.x, interaction.point.x, interaction.point.y)
        when (interaction) {
            is Interaction.Press -> {
                marker = newMarker
                isMouseDown = true
            }
            is Interaction.Move if isMouseDown -> marker = newMarker
            is Interaction.Release -> {
                marker = null
                isMouseDown = false
            }
            else -> {}
        }
    }
}

/** A [Decoration] that captures the chart's layer bounds and coordinate mapping. */
internal class LayerBoundsCapture : Decoration {
    var layerBounds by mutableStateOf(Rect.Zero)
        private set

    /** Padding before the first data point (accounts for column half-width). */
    var startPadding by mutableStateOf(0f)
        private set

    /** Pixel distance between consecutive major x values (pre-scaled by zoom). */
    var xSpacing by mutableStateOf(0f)
        private set

    override fun drawOverLayers(context: CartesianDrawingContext) {
        layerBounds = context.layerBounds
        startPadding = context.layerDimensions.startPadding
        xSpacing = context.layerDimensions.xSpacing
    }

    /** Convert a data x value to pixel x coordinate (assumes minX=0, xStep=1, no scroll). */
    fun dataToPixelX(dataX: Float): Float = layerBounds.left + startPadding + xSpacing * dataX
}

@Composable
internal fun DisplayNear(
    anchorX: Int,
    anchorY: Int,
    modifier: Modifier = Modifier,
    offset: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    val cursorOffsetPx = with(LocalDensity.current) { offset.toPx().toInt() }
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeable = measurables.firstOrNull()?.measure(constraints) ?: return@Layout layout(0, 0) {}

        val parentWidth = constraints.maxWidth
        val parentHeight = constraints.maxHeight
        val childWidth = placeable.width
        val childHeight = placeable.height
        val horizontalPadding = Dimensions.spacingMd.value.toInt()

        val placeBelow = anchorY + childHeight <= parentHeight
        val placeRight = anchorX + cursorOffsetPx + horizontalPadding + childWidth <= parentWidth

        val x =
            if (placeRight) {
                anchorX + cursorOffsetPx + horizontalPadding
            } else {
                anchorX - childWidth - horizontalPadding
            }

        val y =
            if (placeBelow) {
                anchorY
            } else {
                anchorY - childHeight
            }

        layout(parentWidth, constraints.maxHeight) { placeable.placeRelative(x, y) }
    }
}

@Composable
internal fun GuideLine(x: Float, layerBounds: Rect, modifier: Modifier = Modifier, color: Color = Color.Gray) {
    Canvas(modifier = modifier) {
        drawLine(
            color = color,
            start = Offset(x, layerBounds.bottom),
            end = Offset(x, layerBounds.top),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), phase = 0f),
        )
    }
}

private val SteadyStateColor = Color(0xFFFF9800)

@Composable
internal fun SteadyStateGuideLine(x: Float, layerBounds: Rect, modifier: Modifier = Modifier) {
    GuideLine(x, layerBounds, modifier, color = SteadyStateColor)

    // Label at the top of the guideline — two layout variants measured and the best one placed
    Layout(
        modifier = modifier,
        content = {
            // Variant 0: centered above the line, triangle pointing down
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SteadyStatePill()
                TrianglePointer(PointerDirection.Down)
            }
            // Variant 1: to the right of the line, triangle pointing left
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrianglePointer(PointerDirection.Left)
                SteadyStatePill()
            }
        },
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val centered = measurables[0].measure(childConstraints)
        val rightOf = measurables[1].measure(childConstraints)

        val centeredX = (x - centered.width / 2f).toInt()
        val wouldOverlap = centeredX < layerBounds.left.toInt()

        layout(constraints.maxWidth, constraints.maxHeight) {
            if (wouldOverlap) {
                rightOf.placeRelative(x.toInt(), layerBounds.top.toInt())
            } else {
                centered.placeRelative(centeredX, layerBounds.top.toInt())
            }
        }
    }
}

@Composable
private fun SteadyStatePill() {
    Box(
        modifier =
            Modifier.background(SteadyStateColor, shape = RoundedCornerShape(4.dp))
                .padding(horizontal = Dimensions.spacingSm, vertical = Dimensions.spacingXxs)
    ) {
        Text(text = "Steady State", style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

private enum class PointerDirection {
    Down,
    Left,
}

@Composable
private fun TrianglePointer(direction: PointerDirection) {
    Canvas(modifier = Modifier.size(8.dp)) {
        val path =
            Path().apply {
                when (direction) {
                    PointerDirection.Down -> {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width / 2f, size.height)
                    }
                    PointerDirection.Left -> {
                        moveTo(size.width, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height / 2f)
                    }
                }
                close()
            }
        drawPath(path, SteadyStateColor)
    }
}
