package com.group7.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun Pannable(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    Box(modifier = modifier) {
        Box(Modifier.verticalScroll(verticalScrollState).horizontalScroll(horizontalScrollState)) { content() }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(verticalScrollState),
        )
        HorizontalScrollbar(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            adapter = rememberScrollbarAdapter(horizontalScrollState),
        )
    }
}
