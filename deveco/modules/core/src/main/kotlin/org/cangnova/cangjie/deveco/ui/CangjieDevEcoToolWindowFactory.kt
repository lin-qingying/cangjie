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
    /**
     * DevEco 工具窗口在所有项目中可见。
     */
    override fun shouldBeAvailable(project: Project) = true

    /**
     * 创建并注册 DevEco 仓颉运行时状态面板。
     */
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

    /**
     * 在 read action 中读取运行时快照，避免文件索引访问违反 IntelliJ 线程约束。
     */
    private fun readSnapshot(runtimeStatus: DevEcoRuntimeStatus): DevEcoRuntimeSnapshot =
        ApplicationManager.getApplication().runReadAction<DevEcoRuntimeSnapshot> {
            runtimeStatus.snapshot()
        }
}

/**
 * 使用 Swing 构建状态面板，避免 DevEco 宿主缺少 Compose/Skiko 模块时无法加载插件。
 */
private class CangjieDevEcoStatusPanel(
    /** 读取最新运行时快照的函数。 */
    private val snapshotProvider: () -> DevEcoRuntimeSnapshot,
) {
    /** 面板根组件。 */
    val component: JPanel = JPanel(BorderLayout())
    /** 纵向承载状态内容的面板。 */
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

    /**
     * 重新读取快照并刷新 UI。
     */
    private fun refresh() {
        render(snapshotProvider())
    }

    /**
     * 根据运行时快照重建状态面板内容。
     */
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

    /**
     * 添加手动刷新按钮。
     */
    private fun addRefreshButton() {
        val button = JButton(DevEcoMessageBundle.message("toolwindow.cangjieDevEco.refresh.button")).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener { refresh() }
        }
        contentPanel.add(Box.createVerticalStrut(8))
        contentPanel.add(button)
    }

    /**
     * 添加单项运行时能力状态。
     */
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

    /**
     * 添加单项运行时资源状态。
     */
    private fun addResource(status: DevEcoRuntimeResourceStatus) {
        addLine("${statusText(status.present)} ${status.resource.name}")
        if (!status.present) {
            addLine(
                DevEcoMessageBundle.message("toolwindow.cangjieDevEco.resource.missing", status.resource.relativePath),
                indent = true,
            )
        }
    }

    /**
     * 添加面板标题。
     */
    private fun addTitle(text: String) {
        addLabel(text, bold = true)
    }

    /**
     * 添加带分割线的内容分区标题。
     */
    private fun addSection(text: String) {
        contentPanel.add(Box.createVerticalStrut(14))
        contentPanel.add(JSeparator().apply { alignmentX = Component.LEFT_ALIGNMENT })
        contentPanel.add(Box.createVerticalStrut(8))
        addLabel(text, bold = true)
    }

    /**
     * 添加普通状态行。
     */
    private fun addLine(text: String, indent: Boolean = false) {
        addLabel(text, indent = indent)
    }

    /**
     * 添加 Swing 文本标签。
     */
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

/**
 * 将布尔状态转换为本地化可用/不可用文本。
 */
private fun statusText(ok: Boolean): String =
    if (ok) {
        DevEcoMessageBundle.message("toolwindow.cangjieDevEco.status.available")
    } else {
        DevEcoMessageBundle.message("toolwindow.cangjieDevEco.status.unavailable")
    }
