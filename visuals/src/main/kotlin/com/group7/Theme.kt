package com.group7

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SimPortColorScheme = lightColorScheme()

/**
 * Applies the SimPort Material 3 theme to content.
 *
 * @param content the composable content to theme
 */
@Composable
fun SimPortTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SimPortColorScheme, content = content)
}
