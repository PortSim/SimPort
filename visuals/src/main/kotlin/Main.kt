import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.group7.generatePort

fun main() {
    val (scenario) = generatePort(6, 6, 20)

    application { Window(onCloseRequest = ::exitApplication, title = "SimPort") { Visualisation(scenario) } }
}
