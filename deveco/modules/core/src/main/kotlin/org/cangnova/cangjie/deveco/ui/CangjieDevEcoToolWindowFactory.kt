package org.cangnova.cangjie.deveco.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.cangnova.cangjie.deveco.runtime.DevEcoRuntimeCapabilityStatus
import org.cangnova.cangjie.deveco.runtime.DevEcoRuntimeResourceStatus
import org.cangnova.cangjie.deveco.runtime.DevEcoRuntimeSnapshot
import org.cangnova.cangjie.deveco.runtime.DevEcoRuntimeStatus
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator

/**
 * DevEco 侧仓颉增强入口。
 * 该入口展示插件运行时、官方 Cangjie 能力和鸿蒙项目识别状态，便于定位 DevEco 宿主装载问题。
 */
class CangjieDevEcoToolWindowFactory : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val runtimeStatus = DevEcoRuntimeStatus(project)
        val statusPanel = CangjieDevEcoStatusPanel {
            readSnapshot(runtimeStatus)
        }
        val content = ContentFactory.getInstance().createContent(
            statusPanel.component,
            DevEcoMessageBundle.message("toolwindow.stripe.cangjieDevEco"),
            false,
        )
        toolWindow.contentManager.addContent(content)
    }

    private fun readSnapshot(runtimeStatus: DevEcoRuntimeStatus): DevEcoRuntimeSnapshot =
        ApplicationManager.getApplication().runReadAction<DevEcoRuntimeSnapshot> {
            runtimeStatus.snapshot()
        }
}

/**
 * 使用 Swing 构建状态面板，避免 DevEco 宿主缺少 Compose/Skiko 模块时无法加载插件。
 */
private class CangjieDevEcoStatusPanel(
    private val snapshotProvider: () -> DevEcoRuntimeSnapshot,
) {
    val component: JPanel = JPanel(BorderLayout())
    private val contentPanel = JPanel()

    init {
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
        contentPanel.border = JBUI.Borders.empty(16, 20)

        val scrollPane = JBScrollPane(contentPanel).apply {
            border = JBUI.Borders.empty()
        }
        component.add(scrollPane, BorderLayout.CENTER)
        refresh()
    }

    private fun refresh() {
        render(snapshotProvider())
    }

    private fun render(snapshot: DevEcoRuntimeSnapshot) {
        contentPanel.removeAll()
        addTitle(DevEcoMessageBundle.message("toolwindow.cangjieDevEco.runtime.title"))
        addLine(
            DevEcoMessageBundle.message(
                "toolwindow.cangjieDevEco.runtime.summary",
                snapshot.capabilities.count(DevEcoRuntimeCapabilityStatus::available),
                snapshot.capabilities.size,
                snapshot.resources.count(DevEcoRuntimeResourceStatus::present),
                snapshot.resources.size,
            )
        )
        addLine(
            DevEcoMessageBundle.message(
                "toolwindow.cangjieDevEco.runtime.ready",
                statusText(snapshot.runtimeReady),
            )
        )
        addLine(DevEcoMessageBundle.message("toolwindow.cangjieDevEco.plugin.id", snapshot.officialPluginId))
        addLine(
            if (snapshot.officialPluginPath != null) {
                DevEcoMessageBundle.message("toolwindow.cangjieDevEco.plugin.path", snapshot.officialPluginPath)
            } else {
                DevEcoMessageBundle.message("toolwindow.cangjieDevEco.plugin.missing", snapshot.officialPluginId)
            }
        )
        addLine(
            DevEcoMessageBundle.message(
                if (snapshot.harmonyProjectDetected) {
                    "toolwindow.cangjieDevEco.project.detected"
                } else {
                    "toolwindow.cangjieDevEco.project.notDetected"
                }
            )
        )

        if (snapshot.indexing) {
            addLine(DevEcoMessageBundle.message("toolwindow.cangjieDevEco.project.indexing"))
        }

        addRefreshButton()

        addSection(DevEcoMessageBundle.message("toolwindow.cangjieDevEco.capabilities.title"))
        snapshot.capabilities.forEach(::addCapability)

        addSection(DevEcoMessageBundle.message("toolwindow.cangjieDevEco.resources.title"))
        snapshot.resources.forEach(::addResource)

        addSection(DevEcoMessageBundle.message("toolwindow.cangjieDevEco.markers.title"))
        snapshot.projectMarkers.forEach { marker ->
            addLine("${statusText(marker.present)} ${marker.fileName}")
        }

        contentPanel.revalidate()
        contentPanel.repaint()
    }

    private fun addRefreshButton() {
        val button = JButton(DevEcoMessageBundle.message("toolwindow.cangjieDevEco.refresh.button")).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener { refresh() }
        }
        contentPanel.add(Box.createVerticalStrut(8))
        contentPanel.add(button)
    }

    private fun addCapability(status: DevEcoRuntimeCapabilityStatus) {
        addLine("${statusText(status.available)} ${status.capability.name}")
        if (!status.descriptorAvailable) {
            addLine(
                DevEcoMessageBundle.message(
                    "toolwindow.cangjieDevEco.capability.missingDescriptor",
                    status.capability.descriptorPath,
                ),
                indent = true,
            )
        }
        status.missingClasses.forEach { missingClass ->
            addLine(
                DevEcoMessageBundle.message("toolwindow.cangjieDevEco.capability.missingClass", missingClass),
                indent = true,
            )
        }
    }

    private fun addResource(status: DevEcoRuntimeResourceStatus) {
        addLine("${statusText(status.present)} ${status.resource.name}")
        if (!status.present) {
            addLine(
                DevEcoMessageBundle.message("toolwindow.cangjieDevEco.resource.missing", status.resource.relativePath),
                indent = true,
            )
        }
    }

    private fun addTitle(text: String) {
        addLabel(text, bold = true)
    }

    private fun addSection(text: String) {
        contentPanel.add(Box.createVerticalStrut(14))
        contentPanel.add(JSeparator().apply { alignmentX = Component.LEFT_ALIGNMENT })
        contentPanel.add(Box.createVerticalStrut(8))
        addLabel(text, bold = true)
    }

    private fun addLine(text: String, indent: Boolean = false) {
        addLabel(text, indent = indent)
    }

    private fun addLabel(text: String, indent: Boolean = false, bold: Boolean = false) {
        val label = JLabel(text).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.empty(2, if (indent) 18 else 0, 2, 0)
            if (bold) {
                font = font.deriveFont(Font.BOLD)
            }
        }
        contentPanel.add(label)
    }
}

private fun statusText(ok: Boolean): String =
    if (ok) {
        DevEcoMessageBundle.message("toolwindow.cangjieDevEco.status.available")
    } else {
        DevEcoMessageBundle.message("toolwindow.cangjieDevEco.status.unavailable")
    }
