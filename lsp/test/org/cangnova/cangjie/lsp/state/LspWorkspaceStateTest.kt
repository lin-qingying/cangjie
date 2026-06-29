package org.cangnova.cangjie.lsp.state

import org.cangnova.cangjie.lsp.testkit.LspClientCapabilitiesBuilder
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.WorkspaceFolder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 校验 LSP 工作区状态对 initialize、workspace folder 和诊断协商的处理。
 */
class LspWorkspaceStateTest {
    /**
     * 校验 rootUri 可以退化为 workspace folder，并应用库搜索路径系统属性。
     */
    @Test
    fun `initialize falls back to rootUri as workspace folder and applies project properties`() {
        withLibraryPropertiesRestored {
            val state = LspWorkspaceState()
            state.initialize(
                InitializeParams().apply {
                    rootUri = "file:///workspace/root"
                    initializationOptions = mapOf(
                        "stdLibPathOption" to "D:/sdk/stdlib",
                        "targetLib" to "D:/workspace/.cache/lsp",
                    )
                },
            )

            assertEquals(listOf("file:///workspace/root"), state.workspaceFolders().map(WorkspaceFolder::getUri))
            assertEquals("root", state.workspaceFolders().single().name)
            assertEquals("D:/sdk/stdlib", System.getProperty(LspProjectConfiguration.STDLIB_PROPERTY))
            assertEquals("D:/workspace/.cache/lsp", System.getProperty(LspProjectConfiguration.LIBRARY_PROPERTY))
            assertEquals("root", state.projectConfiguration().workspaceModules.single().name)
        }
    }

    /**
     * 校验 workspace folder 增删会同步到当前状态。
     */
    @Test
    fun `updateWorkspaceFolders keeps workspace folder state in sync`() {
        val state = LspWorkspaceState()
        state.initialize(
            InitializeParams().apply {
                workspaceFolders = listOf(WorkspaceFolder("file:///workspace/one", "one"))
            },
        )

        state.updateWorkspaceFolders(
            added = listOf(WorkspaceFolder("file:///workspace/two", "two")),
            removed = listOf(WorkspaceFolder("file:///workspace/one", "one")),
        )

        assertEquals(listOf("file:///workspace/two"), state.workspaceFolders().map(WorkspaceFolder::getUri))
    }

    /**
     * 校验 publishDiagnostics version 字段只在客户端显式协商时启用。
     */
    @Test
    fun `supports publish diagnostics version only when client negotiates it`() {
        val versionedState = LspWorkspaceState()
        versionedState.initialize(
            InitializeParams().apply {
                rootUri = "file:///workspace"
                capabilities = LspClientCapabilitiesBuilder.publishDiagnosticsVersioned()
            },
        )
        assertTrue(versionedState.supportsPublishDiagnosticsVersion())

        val plainState = LspWorkspaceState()
        plainState.initialize(
            InitializeParams().apply {
                rootUri = "file:///workspace"
                capabilities = LspClientCapabilitiesBuilder.core()
            },
        )
        assertFalse(plainState.supportsPublishDiagnosticsVersion())
    }

    /**
     * 校验 shutdown 标记独立于 initialize 生命周期维护。
     */
    @Test
    fun `marks shutdown requested independently from initialize lifecycle`() {
        val state = LspWorkspaceState()
        state.initialize(InitializeParams().apply { rootUri = "file:///workspace" })

        assertFalse(state.isShutdownRequested())
        state.markShutdownRequested()
        assertTrue(state.isShutdownRequested())
    }

    /**
     * 在测试执行后恢复库搜索路径相关系统属性。
     */
    private fun withLibraryPropertiesRestored(action: () -> Unit) {
        val previousStdlib = System.getProperty(LspProjectConfiguration.STDLIB_PROPERTY)
        val previousLibrary = System.getProperty(LspProjectConfiguration.LIBRARY_PROPERTY)
        try {
            action()
        } finally {
            restoreProperty(LspProjectConfiguration.STDLIB_PROPERTY, previousStdlib)
            restoreProperty(LspProjectConfiguration.LIBRARY_PROPERTY, previousLibrary)
        }
    }

    /**
     * 按给定旧值恢复或清除系统属性。
     */
    private fun restoreProperty(key: String, value: String?) {
        if (value == null) {
            System.clearProperty(key)
        } else {
            System.setProperty(key, value)
        }
    }
}
