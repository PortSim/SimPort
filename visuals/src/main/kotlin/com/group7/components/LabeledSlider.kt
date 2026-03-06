package com.group7.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.group7.Dimensions

enum class ValueLabelPosition {
    Left,
    Right,
    Below,
    Hidden,
}

/** A [Slider] with min/max endpoint labels and a configurable value label position. */
@Composable
fun LabeledSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    minLabel: String,
    maxLabel: String,
    valueLabel: String,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    valueLabelPosition: ValueLabelPosition = ValueLabelPosition.Left,
) {
    Column(modifier = modifier.padding(Dimensions.spacingXs), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Conditionally display value label on the left
            if (valueLabelPosition == ValueLabelPosition.Left) {
                Text(text = valueLabel, style = MaterialTheme.typography.bodyMedium)
            }
            // Minimum value label
            Text(
                text = minLabel,
                style = MaterialTheme.typography.bodySmall,
                modifier =
                    if (valueLabelPosition == ValueLabelPosition.Left) Modifier.padding(start = Dimensions.spacingSm)
                    else Modifier,
            )
            // Interactive slider with state callback
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.weight(1f).padding(horizontal = Dimensions.spacingSm),
            )
            // Maximum value label
            Text(text = maxLabel, style = MaterialTheme.typography.bodySmall)
            // Conditionally display value label on the right
            if (valueLabelPosition == ValueLabelPosition.Right) {
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = Dimensions.spacingSm),
                )
            }
        }
        // Conditionally display value label below
        if (valueLabelPosition == ValueLabelPosition.Below) {
            Text(text = valueLabel, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
