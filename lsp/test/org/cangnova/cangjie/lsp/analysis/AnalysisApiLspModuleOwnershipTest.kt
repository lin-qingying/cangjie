@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.lsp.analysis

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.state.LspWorkspaceState
import org.cangnova.cangjie.lsp.testkit.ServiceLoadedLspTestEnvironment
import org.cangnova.cangjie.psi.CjPsiFactory
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.WorkspaceFolder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/** G3：磁盘文件与打开文档 overlay 归属同一真实工作区，跨模块 use-site 不能替换其 owner。 */
class AnalysisApiLspModuleOwnershipTest : AbstractLspIntegrationTest() {
    override val autoCreateDefaultSession: Boolean = false

    @TempDir
    lateinit var directory: Path

    @Test
    fun diskAndOverlayFilesKeepTheirWorkspaceOwners() {
        val dependencyRoot = Files.createDirectories(directory.resolve("dependency"))
        val consumerRoot = Files.createDirectories(directory.resolve("consumer"))
        val dependencyPath = dependencyRoot.resolve("dependency.cj")
        val consumerPath = consumerRoot.resolve("consumer.cj")
        Files.writeString(dependencyPath, "package owner_dependency\npublic func exported(): Int64 { 1 }", StandardCharsets.UTF_8)
        Files.writeString(consumerPath, "package owner_consumer\npublic func consume(): Int64 { 2 }", StandardCharsets.UTF_8)
        val previousStdlib = System.getProperty("cangjie.stdlib.module")
        val previousLibrary = System.getProperty("cangjie.library")
        try {
            ServiceLoadedLspTestEnvironment.create().use { environment ->
                val workspace = LspWorkspaceState().apply {
                    initialize(InitializeParams().apply {
                        workspaceFolders = listOf(
                            WorkspaceFolder(dependencyRoot.toUri().toString(), "dependency"),
                            WorkspaceFolder(consumerRoot.toUri().toString(), "consumer"),
                        )
                    })
                }
                val state = AnalysisApiLspProjectStructureState(environment.project)
                state.configure(workspace)
                val dependencyModule = state.allSourceLikeModules.single { it.name == "dependency" }
                val consumerModule = state.allSourceLikeModules.single { it.name == "consumer" }
                val dependencyFile = state.allSourceFiles.single { it.name == "dependency.cj" }
                val consumerFile = state.allSourceFiles.single { it.name == "consumer.cj" }
                assertSame(dependencyModule, state.getModule(dependencyFile, consumerModule))
                assertSame(consumerModule, state.getModule(consumerFile, dependencyModule))

                val document = LspTextDocument(
                    dependencyPath.toUri().toString(), "cangjie", 2,
                    "package owner_dependency\npublic func updated(): Int64 { 3 }",
                )
                val overlay = CjPsiFactory(environment.project, markGenerated = false)
                    .createFile("dependency.cj", document.analysisText)
                state.upsertOpenDocumentSnapshot(document, overlay)
                state.configure(workspace)
                val currentDependencyModule = state.allSourceLikeModules.single { it.name == "dependency" }
                val currentConsumerModule = state.allSourceLikeModules.single { it.name == "consumer" }
                assertSame(currentDependencyModule, state.getModule(overlay.declarations.single(), currentConsumerModule))
                assertEquals(listOf(overlay), state.allSourceFiles.filter { it.name == "dependency.cj" })
            }
        } finally {
            restoreProperty("cangjie.stdlib.module", previousStdlib)
            restoreProperty("cangjie.library", previousLibrary)
        }
    }

    private fun restoreProperty(name: String, value: String?) {
        if (value == null) System.clearProperty(name) else System.setProperty(name, value)
    }
}
