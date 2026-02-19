package components

import Dimensions
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
internal fun GuideLine(x: Float, layerBounds: Rect, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawLine(
            color = Color.Gray,
            start = Offset(x, layerBounds.bottom),
            end = Offset(x, layerBounds.top),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), phase = 0f),
        )
    }
}
