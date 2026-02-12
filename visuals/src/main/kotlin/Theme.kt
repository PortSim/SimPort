import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SimPortColorScheme = lightColorScheme()

@Composable
fun SimPortTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SimPortColorScheme, content = content)
}
