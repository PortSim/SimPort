package components

import Dimensions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun PlaybackSpeedSlider(currentSpeed: Float, onSpeedChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    // 1. Convert the actual speed (0.01 - 100) to the slider's internal exponent range (-2 to 2)
    // We clamp the input to avoid log10(0) errors
    val sliderValue = remember(currentSpeed) { log10(currentSpeed.coerceAtLeast(0.01f)) }

    Column(modifier = modifier.padding(Dimensions.spacingLg), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "0.01x", style = MaterialTheme.typography.bodySmall)

            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    // 2. Convert the slider's exponent (-2 to 2) back to actual speed
                    // 10^newValue
                    val convertedSpeed = 10f.pow(newValue)
                    onSpeedChange(convertedSpeed)
                },
                valueRange = -2f..2f, // -2 is 0.01, 0 is 1.0, 2 is 100
                modifier = Modifier.weight(1f).padding(horizontal = Dimensions.spacingSm),
            )

            Text(text = "100x", style = MaterialTheme.typography.bodySmall)
        }
        // Display current value formatted nicely
        Text(text = formatSpeed(currentSpeed), style = MaterialTheme.typography.bodyMedium)
    }
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

@Preview(showBackground = true)
@Composable
fun PreviewPlaybackSlider() {
    // State to hold the speed in the parent
    var speed by remember { mutableFloatStateOf(1f) }

    MaterialTheme { PlaybackSpeedSlider(currentSpeed = speed, onSpeedChange = { speed = it }) }
}
