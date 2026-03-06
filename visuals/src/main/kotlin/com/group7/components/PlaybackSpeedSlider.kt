package com.group7.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/** Logarithmic speed slider (0.01x – 100x) that maps the slider position through `10^x`. */
@Composable
fun PlaybackSpeedSlider(currentSpeed: Float, onSpeedChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    // Convert the actual speed (0.01 - 100) to the slider's internal exponent range (-2 to 2)
    // E.g., 0.01 -> log10(0.01) = -2, 1.0 -> log10(1.0) = 0, 100 -> log10(100) = 2
    val sliderValue = remember(currentSpeed) { log10(currentSpeed.coerceAtLeast(0.01f)) }

    LabeledSlider(
        value = sliderValue,
        onValueChange = { newValue ->
            // Convert the slider's exponent (-2 to 2) back to actual speed using 10^x
            // E.g., -2 -> 0.01x, 0 -> 1.0x, 2 -> 100x
            val convertedSpeed = 10f.pow(newValue)
            onSpeedChange(convertedSpeed)
        },
        valueRange = -2f..2f,
        minLabel = "0.01x",
        maxLabel = "100x",
        valueLabel = "Speed: ${formatSpeed(currentSpeed)}",
        valueLabelPosition = ValueLabelPosition.Left,
        modifier = modifier,
    )
}

/**
 * Helper to format the speed string.
 * - Shows fewer decimals for large numbers.
 * - Shows more decimals for small numbers.
 */
fun formatSpeed(speed: Float): String {
    return when {
        speed >= 10 -> "${speed.roundToInt()}x"
        speed >= 1 -> "%.1fx".format(speed)
        else -> "%.2fx".format(speed)
    }
}
