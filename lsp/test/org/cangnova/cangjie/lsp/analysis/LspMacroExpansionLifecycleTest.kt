package org.cangnova.cangjie.lsp.analysis

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getResolutionFacade
import org.cangnova.cangjie.analysis.macro.expansion.MacroSemanticExpansionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDemandClassification
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroResolutionContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSymbolIndex
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.bindMacroImports
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildMacroSymbolIndex
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.addClasspathRoot
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.targetPlatform
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.frontend.pipeline.macroConstructionMode
import org.cangnova.cangjie.lsp.state.LspWorkspaceState
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.cangnova.cangjie.lsp.testkit.ServiceLoadedLspTestEnvironment
import org.cangnova.cangjie.macro.expansion.MacroExpansionArtifactPreparation
import org.cangnova.cangjie.macro.expansion.MacroExpansionOutcome
import org.cangnova.cangjie.macro.expansion.MacroSemanticExpansionEngine
import org.cangnova.cangjie.macro.expansion.ModuleRoots
import org.cangnova.cangjie.macro.expansion.SdkHandle
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Phase 3 生命周期：会话失效会取消在途构造，并通过 `onAbandoned(CANCELLED)` 收尾。
 */
@EnabledOnOs(OS.LINUX)
class LspMacroExpansionLifecycleTest {
    @Test
    fun `session invalidation cancels in-flight construction`() {
        val engine = BlockingEngine()
        val abandoned = AtomicReference<MacroExpansionOutcome?>()
        val options = LspMacroExpansionOptions(
            engine = engine,
            sdkProvider = { null },
            compilerConfigurationProvider = { module, roots, _ -> configuration(module, roots) },
            abandonedListener = { _, outcome -> abandoned.set(outcome) },
        )

        ServiceLoadedLspTestEnvironment.create(
            mode = CangJieCoreEnvironmentMode.UnitTest,
            macroExpansion = options,
        ).use { environment ->
            val project = environment.project
            LspWorkspaceFixtureBuilder()
                .source(
                    "A.cj",
                    """
                    package sample

                    @Make
                    public func carrier(): Unit {}
                    """.trimIndent(),
                )
                .build()
                .use { fixture ->
                    val workspaceState = LspWorkspaceState()
                    workspaceState.initialize(fixture.initializeParams())
                    val structureState = AnalysisApiLspProjectStructureState.getInstance(project)
                    structureState.configure(workspaceState)

                    val sourcePath = fixture.workspaceRoot.resolve("src").resolve("A.cj")
                    val virtualFile = checkNotNull(
                        VirtualFileManager.getInstance().refreshAndFindFileByNioPath(sourcePath),
                    )
                    val psiFile = checkNotNull(
                        PsiManager.getInstance(project).findFile(virtualFile) as? CjFile,
                    )
                    val module = checkNotNull(
                        structureState.getModuleForFile(psiFile),
                    ) as CaSourceModule

                    val session = module.getResolutionFacade(project).sessionProvider.getResolvableSession(module)
                    val service = MacroSemanticExpansionService.getInstance(project)
                    service.request(session)
                    assertTrue(engine.started.await(5, TimeUnit.SECONDS), "构造应进入 expand")

                    Disposer.dispose(session.requestDisposable())
                    service.awaitIdle(module)

                    assertTrue(engine.interrupted, "在途构造应被中断")
                    assertEquals(MacroExpansionOutcome.CANCELLED, abandoned.get())
                    assertEquals(1, service.metrics(module).abandoned)
                }
        }
    }

    @OptIn(CompilerConfiguration.Internals::class)
    @Test
    fun `project disposal cancels in-flight construction`() {
        val engine = BlockingEngine()
        val abandoned = AtomicReference<MacroExpansionOutcome?>()
        val options = LspMacroExpansionOptions(
            engine = engine,
            sdkProvider = { null },
            compilerConfigurationProvider = { module, roots, _ -> configuration(module, roots) },
            abandonedListener = { _, outcome -> abandoned.set(outcome) },
        )

        ServiceLoadedLspTestEnvironment.create(
            mode = CangJieCoreEnvironmentMode.UnitTest,
            macroExpansion = options,
        ).use { environment ->
            val project = environment.project
            LspWorkspaceFixtureBuilder()
                .source(
                    "A.cj",
                    """
                    package sample

                    @Make
                    public func carrier(): Unit {}
                    """.trimIndent(),
                )
                .build()
                .use { fixture ->
                    val workspaceState = LspWorkspaceState()
                    workspaceState.initialize(fixture.initializeParams())
                    val structureState = AnalysisApiLspProjectStructureState.getInstance(project)
                    structureState.configure(workspaceState)

                    val sourcePath = fixture.workspaceRoot.resolve("src").resolve("A.cj")
                    val virtualFile = checkNotNull(
                        VirtualFileManager.getInstance().refreshAndFindFileByNioPath(sourcePath),
                    )
                    val psiFile = checkNotNull(
                        PsiManager.getInstance(project).findFile(virtualFile) as? CjFile,
                    )
                    val module = checkNotNull(
                        structureState.getModuleForFile(psiFile),
                    ) as CaSourceModule

                    val session = module.getResolutionFacade(project).sessionProvider.getResolvableSession(module)
                    val service = MacroSemanticExpansionService.getInstance(project)
                    service.request(session)
                    assertTrue(engine.started.await(5, TimeUnit.SECONDS), "构造应进入 expand")

                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait {
                        com.intellij.openapi.application.WriteAction.compute<Unit, RuntimeException> {
                            Disposer.dispose(project)
                        }
                    }
                    engine.release.countDown()
                    service.awaitIdle(module)

                    assertEquals(MacroExpansionOutcome.CANCELLED, abandoned.get())
                    assertEquals(1, service.metrics(module).abandoned)
                }
        }
    }

    @Test
    fun `write during construction does not publish incomplete snapshot`() {
        val engine = BlockingEngine()
        val abandoned = AtomicReference<MacroExpansionOutcome?>()
        val options = LspMacroExpansionOptions(
            engine = engine,
            sdkProvider = { null },
            compilerConfigurationProvider = { module, roots, _ -> configuration(module, roots) },
            abandonedListener = { _, outcome -> abandoned.set(outcome) },
        )

        ServiceLoadedLspTestEnvironment.create(
            mode = CangJieCoreEnvironmentMode.UnitTest,
            macroExpansion = options,
        ).use { environment ->
            val project = environment.project
            LspWorkspaceFixtureBuilder()
                .source(
                    "A.cj",
                    """
                    package sample

                    @Make
                    public func carrier(): Unit {}
                    """.trimIndent(),
                )
                .build()
                .use { fixture ->
                    val workspaceState = LspWorkspaceState()
                    workspaceState.initialize(fixture.initializeParams())
                    val structureState = AnalysisApiLspProjectStructureState.getInstance(project)
                    structureState.configure(workspaceState)

                    val sourcePath = fixture.workspaceRoot.resolve("src").resolve("A.cj")
                    val virtualFile = checkNotNull(
                        VirtualFileManager.getInstance().refreshAndFindFileByNioPath(sourcePath),
                    )
                    val psiFile = checkNotNull(
                        PsiManager.getInstance(project).findFile(virtualFile) as? CjFile,
                    )
                    val module = checkNotNull(
                        structureState.getModuleForFile(psiFile),
                    ) as CaSourceModule

                    val session = module.getResolutionFacade(project).sessionProvider.getResolvableSession(module)
                    val service = MacroSemanticExpansionService.getInstance(project)
                    service.request(session)
                    assertTrue(engine.started.await(5, TimeUnit.SECONDS), "构造应进入 expand")

                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait {
                        com.intellij.openapi.application.WriteAction.compute<Unit, RuntimeException> {
                            structureState.configure(workspaceState)
                        }
                    }
                    engine.release.countDown()
                    service.awaitIdle(module)

                    assertNull(session.macroSemanticSnapshot, "输入变化后不应发布旧快照")
                    assertTrue(
                        abandoned.get() != MacroExpansionOutcome.PUBLISHED,
                        "构造期间写动作后不应发布：${abandoned.get()}",
                    )
                }
        }
    }

    @OptIn(CompilerConfiguration.Internals::class)
    private fun configuration(
        module: CaSourceModule,
        roots: ModuleRoots,
    ): CompilerConfiguration = CompilerConfiguration().apply {
        languageVersionSettings = module.languageVersionSettings
        targetPlatform = module.targetPlatform
        moduleName = module.name
        useLightTree = false
        macroConstructionMode = MacroConstructionService.Mode.DEGRADED
        roots.sourceRoots.forEach { addCangJieSourceRoot(it) }
        roots.classpathRoots.forEach { addClasspathRoot(it) }
    }

    /** 在 expand 处阻塞，直到被中断，用于模拟慢构造。 */
    private class BlockingEngine : MacroSemanticExpansionEngine {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)

        @Volatile
        var interrupted: Boolean = false

        override fun prepareArtifacts(
            configuration: CompilerConfiguration,
            classifications: List<MacroDemandClassification>,
        ): MacroExpansionArtifactPreparation = MacroExpansionArtifactPreparation()

        override fun buildSymbolIndex(
            pre: PreMacroRawBuildResult,
            artifacts: MacroExpansionArtifactPreparation,
        ): MacroSymbolIndex = buildMacroSymbolIndex(pre, macroArtifactDefinitions = artifacts.definitions)

        override fun bindImports(
            pre: PreMacroRawBuildResult,
            index: MacroSymbolIndex,
        ): MacroResolutionContext = bindMacroImports(pre, index)

        override fun expand(
            pre: PreMacroRawBuildResult,
            context: MacroResolutionContext,
            classification: MacroDemandClassification,
            configuration: CompilerConfiguration,
            diagnostics: List<MacroConstructionDiagnostic>,
            mode: MacroConstructionService.Mode,
        ): MacroConstructionResult {
            started.countDown()
            try {
                release.await(30, TimeUnit.SECONDS)
            } catch (_: InterruptedException) {
                interrupted = true
                Thread.currentThread().interrupt()
                throw ProcessCanceledException()
            }
            return MacroConstructionResult.ExecutorUnavailable(MacroExpansionRegistry.EMPTY)
        }
    }
}
