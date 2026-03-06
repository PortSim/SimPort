package com.group7.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private val durationUnits =
    listOf(DurationUnit.MILLISECONDS, DurationUnit.SECONDS, DurationUnit.MINUTES, DurationUnit.HOURS, DurationUnit.DAYS)

/** Numeric text field paired with a unit dropdown for entering a [Duration]. Returns `null` on invalid input. */
@Composable
fun DurationPicker(
    duration: Duration?,
    defaultStepUnit: DurationUnit = DurationUnit.DAYS,
    onDurationChange: (Duration?, DurationUnit) -> Unit,
) {
    // Track selected time unit and numeric input text
    var unit by remember { mutableStateOf(defaultStepUnit) }
    var text by remember { mutableStateOf(duration?.toLong(unit)?.toString() ?: "") }

    Row {
        // Numeric input field for duration value
        OutlinedTextField(
            value = text,
            onValueChange = { new ->
                text = new
                // Parse numeric value and convert to selected unit
                onDurationChange(new.toLongOrNull()?.toDuration(unit), unit)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = duration == null,
            label = { Text("Duration") },
            modifier = Modifier.widthIn(min = 50.dp),
        )

        // Unit selector dropdown
        Dropdown(
            options = durationUnits,
            selected = unit,
            onSelected = {
                unit = it
                // Reparse text with new unit when unit changes
                onDurationChange(text.toLongOrNull()?.toDuration(it), unit)
            },
            label = { Text("Unit") },
            displayText = { it.name.lowercase() },
        )
    }
}
