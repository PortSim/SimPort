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
            if (valueLabelPosition == ValueLabelPosition.Left) {
                Text(text = valueLabel, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = minLabel,
                style = MaterialTheme.typography.bodySmall,
                modifier =
                    if (valueLabelPosition == ValueLabelPosition.Left) Modifier.padding(start = Dimensions.spacingSm)
                    else Modifier,
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.weight(1f).padding(horizontal = Dimensions.spacingSm),
            )
            Text(text = maxLabel, style = MaterialTheme.typography.bodySmall)
            if (valueLabelPosition == ValueLabelPosition.Right) {
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = Dimensions.spacingSm),
                )
            }
        }
        if (valueLabelPosition == ValueLabelPosition.Below) {
            Text(text = valueLabel, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
