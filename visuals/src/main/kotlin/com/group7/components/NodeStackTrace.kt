package com.group7.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.group7.Dimensions
import com.group7.NodeGroup
import java.io.FileNotFoundException
import java.net.ConnectException
import java.net.URI
import javax.swing.JOptionPane

@Composable
fun NodeStackTrace(node: NodeGroup, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXxs)) {
        for (element in filterStackTrace(node.stackTrace)) {
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                ProvideTextStyle(TextStyle(fontSize = Dimensions.fontSizeSmall, fontFamily = FontFamily.Monospace)) {
                    Text("${element.className}.${element.methodName}")
                    element.fileName?.let { fileName ->
                        Text("(")
                        Text(
                            "$fileName:${element.lineNumber}",
                            color = Color.Blue,
                            textDecoration = TextDecoration.Underline,
                            modifier =
                                Modifier.pointerHoverIcon(PointerIcon.Hand).clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) {
                                    openSourceInIntelliJ(element)
                                },
                        )
                        Text(")")
                    }
                }
            }
        }
    }
}

private fun filterStackTrace(stackTrace: List<StackTraceElement>) =
    stackTrace
        .asSequence()
        .dropWhile { it.methodName == "<init>" }
        .dropWhile { it.isInternal }
        .takeWhile { it.className != "com.group7.dsl.ScenarioDslKt" || it.methodName != "buildScenario" }

private val StackTraceElement.isInternal
    get() =
        className.startsWith("com.group7.dsl") ||
            className.startsWith("com.group7.utils") ||
            className.startsWith("com.group7.compound")

private fun openSourceInIntelliJ(frame: StackTraceElement) {
    val dir = frame.className.substringBeforeLast(".").replace(".", "/")
    val sourceFile = dir + "/" + frame.fileName
    val line = frame.lineNumber
    try {
        val url = URI.create("http://localhost:63342/api/file?file=$sourceFile&line=$line").toURL()
        val conn = url.openConnection()
        conn.setRequestProperty("Referer", "http://localhost")
        conn.connect()
        conn.inputStream.close()
    } catch (_: ConnectException) {} catch (_: FileNotFoundException) {
        JOptionPane.showMessageDialog(
            null,
            "Install the `IDE Remote Control` plugin for these links to work",
            "IntelliJ Support",
            JOptionPane.INFORMATION_MESSAGE,
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
