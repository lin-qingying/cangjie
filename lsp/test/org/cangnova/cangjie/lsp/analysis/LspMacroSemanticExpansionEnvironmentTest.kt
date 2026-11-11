package org.cangnova.cangjie.lsp.analysis

import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.macro.expansion.FakeMacroSemanticExpansionEngine
import org.cangnova.cangjie.analysis.macro.expansion.MacroSemanticExpansionEnvironment
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.cangjieSourceRoots
import org.cangnova.cangjie.config.classpathRoots
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.frontend.pipeline.FrontendMacroSemanticExpansionEngine
import org.cangnova.cangjie.frontend.pipeline.macroConstructionMode
import org.cangnova.cangjie.frontend.pipeline.macroExecutorFactory
import org.cangnova.cangjie.frontend.pipeline.macroExpansionDemandAutoCompilationEnabled
import org.cangnova.cangjie.frontend.pipeline.macroFragmentParserFactory
import org.cangnova.cangjie.frontend.pipeline.macroPackageCompilationOrchestrator
import org.cangnova.cangjie.frontend.pipeline.macroSdkHome
import org.cangnova.cangjie.lsp.state.LspWorkspaceState
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.cangnova.cangjie.lsp.testkit.ServiceLoadedLspTestEnvironment
import org.cangnova.cangjie.macro.expansion.MacroConstructionReadAccess
import org.cangnova.cangjie.macro.expansion.SdkHandle
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * LSP 宏展开环境接线：未提供 [LspMacroExpansionOptions] 时保持无宏展开；
 * 提供后 environment 注册成功，且引擎 / 读访问 / 屏障 / roots / revision 按宿主契约工作。
 */
class LspMacroSemanticExpansionEnvironmentTest {
    @Test
    fun `environment is absent without macro expansion options`() {
        ServiceLoadedLspTestEnvironment.create(mode = CangJieCoreEnvironmentMode.UnitTest).use { environment ->
            assertNull(
                MacroSemanticExpansionEnvironment.getInstance(environment.project),
                "未配置 macroExpansion 时 LSP 不应注册 environment（默认行为不变）",
            )
        }
    }

    @Test
    fun `environment is registered and delegates engine and barrier`() {
        val engine = FakeMacroSemanticExpansionEngine()
        val options = macroExpansionOptions(engine)

        ServiceLoadedLspTestEnvironment.create(
            mode = CangJieCoreEnvironmentMode.UnitTest,
            macroExpansion = options,
        ).use { environment ->
            val registered = checkNotNull(MacroSemanticExpansionEnvironment.getInstance(environment.project)) {
                "提供 macroExpansion 后必须注册 LSP environment"
            }
            assertTrue(
                registered is LspMacroSemanticExpansionEnvironment,
                "LSP 必须注册自己的 environment 实现",
            )
            assertEquals(engine, registered.engine())
            assertEquals(MacroConstructionReadAccess.Direct, registered.readAccess())

            // 发布屏障在 headless 容器内可用，并透传 block 的结果。
            assertEquals(7, registered.barrier().publish { 7 })
        }
    }

    /**
     * roots / revision 必须来自真实 LSP 工作区模块与 `AnalysisApiLspProjectStructureState`。
     *
     * 覆盖此前没有用例验证的两段映射：`psiRoots` → `ModuleRoots.sourceRoots`，
     * 以及模块/全局修改计数 → `MacroExpansionRevision`（重新 configure 会递增计数）。
     */
    @Test
    fun `roots and revision follow lsp workspace module`() {
        ServiceLoadedLspTestEnvironment.create(
            mode = CangJieCoreEnvironmentMode.UnitTest,
            macroExpansion = macroExpansionOptions(FakeMacroSemanticExpansionEngine()),
        ).use { environment ->
            val project = environment.project
            LspWorkspaceFixtureBuilder()
                .source("A.cj", "package sample\n\npublic func a(): Int64 { return 1 }\n")
                .build()
                .use { fixture ->
                    val workspaceState = LspWorkspaceState()
                    workspaceState.initialize(fixture.initializeParams())
                    val structureState = AnalysisApiLspProjectStructureState.getInstance(project)
                    structureState.configure(workspaceState)

                    val sourcePath = fixture.workspaceRoot.resolve("src").resolve("A.cj")
                    val virtualFile = checkNotNull(
                        VirtualFileManager.getInstance().refreshAndFindFileByNioPath(sourcePath),
                    ) { "工作区源码文件必须落在 VFS：$sourcePath" }
                    val psiFile = checkNotNull(
                        PsiManager.getInstance(project).findFile(virtualFile) as? CjFile,
                    ) { "工作区源码文件必须解析为 CjFile" }
                    val module = checkNotNull(
                        structureState.getModuleForFile(psiFile),
                    ) { "工作区源码文件必须映射到 LSP CaSourceModule" } as CaSourceModule

                    val registered = checkNotNull(
                        MacroSemanticExpansionEnvironment.getInstance(project),
                    ) as LspMacroSemanticExpansionEnvironment

                    val roots = registered.roots(module)
                    assertTrue(
                        roots.sourceRoots.any { it.replace('\\', '/').endsWith("/src") },
                        "源码根必须来自模块 psiRoots：roots=$roots",
                    )

                    val revision = registered.revision(module)
                    assertEquals(revision, registered.revision(module), "同一输入下 revision 必须相等")

                    // 重新 configure 会重建模块并递增 LSP 修改计数，revision 必须随之变化。
                    structureState.configure(workspaceState)
                    val refreshedModule = checkNotNull(
                        structureState.getModuleForFile(psiFile),
                    ) as CaSourceModule
                    assertNotEquals(
                        revision,
                        registered.revision(refreshedModule),
                        "工作区变更后 revision 必须变化，否则旧快照不会失效",
                    )
                }
        }
    }

    /**
     * 进程内前端装配：`frontendMacroExpansionOptions` 必须只使用 [SdkHandle] 携带的值，
     * 并正确写入宏 SDK、模式、源码根、stdlib classpath 与执行器工厂。
     */
    @Test
    fun `frontend defaults assemble config from sdk handle`() {
        val sdk = SdkHandle(
            id = "lsp-test-sdk",
            homePath = "/fake/sdk",
            environment = mapOf("CANGJIE_HOME" to "/fake/sdk"),
            stdlibPath = "/fake/sdk/modules/plat",
            macroServerPath = "/fake/sdk/tools/bin/LSPMacroServer",
        )
        var invalidatedModule: CaSourceModule? = null
        val options = frontendMacroExpansionOptions(sdk).copy(
            invalidationListener = { invalidatedModule = it },
        )
        ServiceLoadedLspTestEnvironment.create(
            mode = CangJieCoreEnvironmentMode.UnitTest,
            macroExpansion = options,
        ).use { environment ->
            val project = environment.project
            LspWorkspaceFixtureBuilder()
                .source("A.cj", "package sample\n\npublic func a(): Int64 { return 1 }\n")
                .build()
                .use { fixture ->
                    val workspaceState = LspWorkspaceState()
                    workspaceState.initialize(fixture.initializeParams())
                    val structureState = AnalysisApiLspProjectStructureState.getInstance(project)
                    structureState.configure(workspaceState)

                    val sourcePath = fixture.workspaceRoot.resolve("src").resolve("A.cj")
                    val virtualFile = checkNotNull(
                        VirtualFileManager.getInstance().refreshAndFindFileByNioPath(sourcePath),
                    ) { "工作区源码文件必须落在 VFS：$sourcePath" }
                    val psiFile = checkNotNull(
                        PsiManager.getInstance(project).findFile(virtualFile) as? CjFile,
                    ) { "工作区源码文件必须解析为 CjFile" }
                    val module = checkNotNull(
                        structureState.getModuleForFile(psiFile),
                    ) { "工作区源码文件必须映射到 LSP CaSourceModule" } as CaSourceModule
                    val registered = checkNotNull(
                        MacroSemanticExpansionEnvironment.getInstance(project),
                    ) as LspMacroSemanticExpansionEnvironment

                    assertTrue(registered.engine() is FrontendMacroSemanticExpansionEngine)
                    assertEquals(sdk, registered.sdk(module))
                    registered.onSnapshotInvalidated(module)
                    assertEquals(module, invalidatedModule)

                    val roots = registered.roots(module)
                    val configuration = registered.compilerConfiguration(module, roots, sdk)
                    assertEquals(sdk.homePath, configuration.macroSdkHome)
                    assertEquals(MacroConstructionService.Mode.DEGRADED, configuration.macroConstructionMode)
                    assertTrue(configuration.macroExpansionDemandAutoCompilationEnabled)
                    assertNotNull(configuration.macroPackageCompilationOrchestrator)
                    assertNotNull(configuration.macroExecutorFactory)
                    assertTrue(
                        configuration.cangjieSourceRoots.any { it.path.replace('\\', '/').endsWith("/src") },
                        "源码根必须写入配置：${configuration.cangjieSourceRoots.map { it.path }}",
                    )
                    assertTrue(
                        configuration.classpathRoots.any { it.path == sdk.stdlibPath },
                        "stdlib 必须写入 classpath：${configuration.classpathRoots.map { it.path }}",
                    )

                    registered.installFragmentParser(module, configuration)
                    assertNotNull(configuration.macroFragmentParserFactory)
                }
        }
    }

    @OptIn(CompilerConfiguration.Internals::class)
    private fun macroExpansionOptions(
        engine: org.cangnova.cangjie.macro.expansion.MacroSemanticExpansionEngine,
    ): LspMacroExpansionOptions =
        LspMacroExpansionOptions(
            engine = engine,
            compilerConfigurationProvider = { _, _, _ -> CompilerConfiguration() },
        )
}
