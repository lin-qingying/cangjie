package org.cangnova.cangjie.deveco.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.bridge.addComposeTab
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import kotlin.random.Random

/**
 * DevEco 侧仓颉增强入口。
 * 当前先提供一套可同步、可装载的工具窗口骨架，后续分析能力统一从 core 模块继续扩展。
 */
class CangjieDevEcoToolWindowFactory : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.addComposeTab(DevEcoMessageBundle.message("toolwindow.stripe.cangjieDevEco"), focusOnClickInside = true) {
            LaunchedEffect(Unit) {
                // 这里预留为后续仓颉项目模型、诊断和意图动作的数据装载入口。
            }
            CangjieDevEcoToolWindowContent()
        }
    }
}

@Composable
@Preview
private fun CangjieDevEcoToolWindowContent() {
    val labelText = remember {
        mutableStateOf(DevEcoMessageBundle.message("toolwindow.cangjieDevEco.number.label", "?"))
    }

    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(labelText.value)

        OutlinedButton(onClick = {
            labelText.value = DevEcoMessageBundle.message(
                "toolwindow.cangjieDevEco.number.label",
                Random(System.currentTimeMillis()).nextInt(1000),
            )
        }) { Text(DevEcoMessageBundle.message("toolwindow.cangjieDevEco.shuffle.button")) }
    }
}
