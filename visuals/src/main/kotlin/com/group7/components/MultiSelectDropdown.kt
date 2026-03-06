package com.group7.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.group7.Dimensions
import kotlinx.collections.immutable.PersistentSet

/**
 * A dropdown that allows selecting multiple items via checkboxes, with a "Select All / Deselect All" toggle.
 *
 * The text field shows a summary like "Label (3 of 5)". Selection state is managed externally via [selectedOptions] (a
 * [PersistentSet]) and [onSelectionChange].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MultiSelectDropdown(
    label: String,
    options: List<T>,
    selectedOptions: PersistentSet<T>,
    onSelectionChange: (PersistentSet<T>) -> Unit,
    optionLabel: (T) -> String = { it.toString() },
    modifier: Modifier = Modifier,
) {
    // Track whether dropdown menu is open
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        // Display read-only text field showing selection count summary
        OutlinedTextField(
            value = "$label (${selectedOptions.size} of ${options.size})",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors =
                OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            modifier =
                Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .pointerHoverIcon(PointerIcon.Default, overrideDescendants = true),
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            // Select All / Deselect All toggle at the top of menu
            val allSelected = selectedOptions.size == options.size
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSm),
                    ) {
                        // Checkbox shows all-selected state
                        Checkbox(checked = allSelected, onCheckedChange = null)
                        Text(
                            if (allSelected) "Deselect all" else "Select all",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                onClick = {
                    // Toggle between select all and deselect all
                    onSelectionChange(
                        if (allSelected) {
                            selectedOptions.removeAll(selectedOptions)
                        } else {
                            selectedOptions.addAll(options)
                        }
                    )
                },
            )
            HorizontalDivider()

            // Render individual option items with checkboxes
            options.forEach { option ->
                val isSelected = option in selectedOptions
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSm),
                        ) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Text(optionLabel(option))
                        }
                    },
                    onClick = {
                        val newSelection =
                            if (isSelected) {
                                selectedOptions.remove(option)
                            } else {
                                selectedOptions.add(option)
                            }
                        onSelectionChange(newSelection)
                    },
                )
            }
        }
    }
}
