package components

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.toSize

// Calculates the clamped offset to keep a percentage of the inner box always on screen
fun clampOffsetToKeepCanvasOnScreen(
    outerBoxSize: Size,
    innerBoxSize: Size,
    offset: Offset,
    scale: Float,
    onScreenPercentage: Float,
): Offset {
    // Keep 20% of the canvas on screen or keep the screen 20% canvas whichever is possible
    fun calcClampValues(innerSize: Float, outerSize: Float): ClosedFloatingPointRange<Float> {
        val realInnerSize = innerSize * scale

        return if (realInnerSize >= outerSize) {
            val max = (realInnerSize - 2.0f * (onScreenPercentage - 0.5f) * outerSize) / 2.0f
            (-max)..(max)
        } else {
            val max = (outerSize - 2.0f * (onScreenPercentage - 0.5f) * realInnerSize) / 2.0f
            (-max)..(max)
        }
    }
    val xRange = calcClampValues(innerBoxSize.width, outerBoxSize.width)
    val yRange = calcClampValues(innerBoxSize.height, outerBoxSize.height)
    return Offset(offset.x.coerceIn(xRange), offset.y.coerceIn(yRange))
}

@Composable
fun DraggableZoomableBox(content: @Composable () -> Unit) {
    val percentageOfInnerBoxToKeepInScreen = 0.2f
    var scale by remember { mutableStateOf(1.0f) }
    var viewOffset by remember { mutableStateOf(Offset.Zero) }
    var innerElementSize by remember { mutableStateOf(Size.Zero) }
    var outerElementSize by remember { mutableStateOf(Size.Zero) }

    Box(
        Modifier.fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val change = event.changes.first()
                            val zoomMultiplier = (1 - change.scrollDelta.y * 0.1f)
                            val newScale = (scale * zoomMultiplier).coerceIn(0.1f, 20f)

                            // Recalculate zoomFactor based on the clamped newScale to prevent offset jumps
                            val effectiveZoomFactor = newScale / scale

                            // Calculate where the mouse is relative to the center (matching your original logic)
                            // change.position gives coordinates relative to the top-left of the modifier
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val mouseOffset = change.position - center

                            // Keep the point under the mouse stationary
                            viewOffset += (mouseOffset - viewOffset) * (1 - effectiveZoomFactor)
                            scale = newScale

                            viewOffset =
                                clampOffsetToKeepCanvasOnScreen(
                                    outerElementSize,
                                    innerElementSize,
                                    viewOffset,
                                    scale,
                                    percentageOfInnerBoxToKeepInScreen,
                                )
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, _, _ ->
                    viewOffset =
                        clampOffsetToKeepCanvasOnScreen(
                            outerElementSize,
                            innerElementSize,
                            viewOffset + pan,
                            scale,
                            percentageOfInnerBoxToKeepInScreen,
                        )
                }
            }
            .graphicsLayer(translationX = viewOffset.x, translationY = viewOffset.y, scaleX = scale, scaleY = scale)
            .onSizeChanged { size -> outerElementSize = size.toSize() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.layout { measurable, _ ->
                    // 1. Measure the child with infinite constraints
                    val placeable =
                        measurable.measure(
                            Constraints(maxWidth = Constraints.Infinity, maxHeight = Constraints.Infinity)
                        )
                    // 2. Update your state with the "true" unconstrained size
                    innerElementSize = Size(placeable.width.toFloat(), placeable.height.toFloat())
                    // 3. Report the size to the parent layout
                    layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
                }
                .wrapContentSize(unbounded = true)
        ) {
            content()
        }
    }
}
