package com.group7

import androidx.compose.runtime.Composable

/** Represents a visual icon for a node that can be rendered as a Composable. */
interface NodeIcon {
    /** Renders the icon content in Compose. */
    @Composable fun content()
}
