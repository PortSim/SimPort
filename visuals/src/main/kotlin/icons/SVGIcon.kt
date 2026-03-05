package icons

import NodeIcon
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

class SVGIcon(private val resource: DrawableResource) : NodeIcon {
    @Composable
    override fun content() {
        val painter = painterResource(resource)

        BoxWithConstraints {
            Image(
                painter = painter,
                contentDescription = "Node Icon",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        }
    }
}
