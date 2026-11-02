package org.cangnova.cangjie.lsp.analysis

import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getResolutionFacade
import org.cangnova.cangjie.analysis.macro.expansion.MacroSemanticExpansionEnvironment
import org.cangnova.cangjie.analysis.macro.expansion.MacroSemanticExpansionService
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.addClasspathRoot
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.targetPlatform
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.frontend.pipeline.FrontendMacroSemanticExpansionEngine
import org.cangnova.cangjie.frontend.pipeline.MacroArtifactPackage
import org.cangnova.cangjie.frontend.pipeline.macroArtifactPackages
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.frontend.pipeline.MacroCompilationCacheContext
import org.cangnova.cangjie.frontend.pipeline.MacroExecutorFactory
import org.cangnova.cangjie.frontend.pipeline.installDefaultMacroFragmentParserFactory
import org.cangnova.cangjie.frontend.pipeline.macroCompilationCacheContext
import org.cangnova.cangjie.frontend.pipeline.macroCompilerEnvironment
import org.cangnova.cangjie.frontend.pipeline.macroConstructionMode
import org.cangnova.cangjie.frontend.pipeline.macroConstructionReadAccess
import org.cangnova.cangjie.frontend.pipeline.macroExecutorFactory
import org.cangnova.cangjie.frontend.pipeline.macroExpansionDemandAutoCompilationEnabled
import org.cangnova.cangjie.frontend.pipeline.macroSdkHome
import org.cangnova.cangjie.lsp.state.LspWorkspaceState
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.cangnova.cangjie.lsp.testkit.ServiceLoadedLspTestEnvironment
import org.cangnova.cangjie.macro.expansion.MacroConstructionReadAccess
import org.cangnova.cangjie.macro.expansion.MacroExpansionOutcome
import org.cangnova.cangjie.macro.expansion.SdkHandle
import org.cangnova.cangjie.macro.process.LspMacroServerMacroExecutor
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * LSP 真实宏展开端到端：预编译宏包 artifact + 真实 `LSPMacroServer` + 进程内 frontend 引擎。
 *
 * 只在 Linux 运行；Windows 的服务端初始化问题见计划 2.5。
 */
@EnabledOnOs(OS.LINUX)
class LspMacroExpansionRealEngineTest {
    @Test
    fun `real sdk macro expands inside lsp workspace`(@TempDir root: File) {
        val sdkHome = System.getenv("CANGJIE_HOME")
        assumeTrue(sdkHome != null && File(sdkHome).isDirectory, "需要 CANGJIE_HOME 指向 Linux SDK")
        val sdk = File(sdkHome)
        val macroServer = File(sdk, "tools/bin/LSPMacroServer")
        assumeTrue(macroServer.isFile, "SDK 缺少 LSPMacroServer")

        val macroSource = File(root, "macro-src").apply { mkdirs() }
        File(macroSource, "macros.cj").writeText(
            """
            macro package demo_macro

            import std.ast.*

            public macro Make(input: Tokens): Tokens {
                return quote(func generatedByMacro(): Int64 { return 42 })
            }
            """.trimIndent(),
        )
        val macroOut = File(root, "macro-out").apply { mkdirs() }
        val compile = ProcessBuilder(
            File(sdk, "bin/cjc").absolutePath,
            "-p", macroSource.absolutePath,
            "--compile-macro",
        ).directory(macroOut)
            .redirectErrorStream(true)
            .also { it.environment()["CANGJIE_HOME"] = sdk.absolutePath }
            .start()
        val compileOutput = compile.inputStream.bufferedReader().readText()
        assumeTrue(compile.waitFor() == 0, "宏包编译失败: $compileOutput")

        val options = LspMacroExpansionOptions(
            engine = FrontendMacroSemanticExpansionEngine(),
            sdkProvider = { sdkHandle(sdk) },
            compilerConfigurationProvider = { module, roots, _ ->
                compilerConfiguration(module, roots, sdk, macroOut)
            },
            fragmentParserInstaller = { module, configuration ->
                configuration.installDefaultMacroFragmentParserFactory(module.project)
            },
            externalMacroPackagesProvider = { setOf(FqName("demo_macro")) },
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

                    import demo_macro.Make

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

                    assertNotNull(MacroSemanticExpansionEnvironment.getInstance(project))
                    val session = module.getResolutionFacade(project).sessionProvider.getResolvableSession(module)
                    val service = MacroSemanticExpansionService.getInstance(project)
                    service.request(session)
                    service.awaitIdle(module)

                    val snapshot = checkNotNull(session.macroSemanticSnapshot) {
                        "真实宏展开后必须发布宏语义快照"
                    }
                    val generatedNames = snapshot.callableNames(FqName("sample")).map { it.asString() }
                    assertTrue(
                        generatedNames.contains("generatedByMacro"),
                        "宏生成声明应进入快照：$generatedNames",
                    )
                    val generatedCallables = snapshot.callables(
                        CallableId(FqName("sample"), Name.identifier("generatedByMacro")),
                    )
                    assertTrue(
                        generatedCallables.isNotEmpty(),
                        "宏生成声明应可按 CallableId 解析",
                    )
                    val surfaceIds = checkNotNull(
                        snapshot.macroSurfaceIdsForDeclaration(generatedCallables.first()),
                    ) { "宏生成声明应映射回宏调用 surface" }
                    assertTrue(surfaceIds.isNotEmpty())
                    assertTrue(surfaceIds.all { snapshot.originSurface(it) != null })
                    val metrics = service.metrics(module)
                    assertTrue(metrics.constructionsStarted >= 1, "应记录构造次数")
                    assertTrue(metrics.published >= 1, "应记录成功发布次数")
                    assertEquals(MacroExpansionOutcome.PUBLISHED, metrics.lastOutcome)
                    assertTrue(
                        File(macroOut, "lib-macro_demo_macro.so").isFile,
                        "宏包应产出动态库",
                    )
                }
        }
    }

    private fun sdkHandle(sdk: File): SdkHandle = SdkHandle(
        id = "test:${sdk.absolutePath}",
        homePath = sdk.absolutePath,
        environment = System.getenv() + mapOf("CANGJIE_HOME" to sdk.absolutePath),
        macroServerPath = File(sdk, "tools/bin/LSPMacroServer").absolutePath,
    )

    @OptIn(CompilerConfiguration.Internals::class)
    private fun compilerConfiguration(
        module: CaSourceModule,
        roots: org.cangnova.cangjie.macro.expansion.ModuleRoots,
        sdk: File,
        macroOut: File,
    ): CompilerConfiguration = CompilerConfiguration().apply {
        languageVersionSettings = module.languageVersionSettings
        targetPlatform = module.targetPlatform
        moduleName = module.name
        useLightTree = false
        macroConstructionMode = MacroConstructionService.Mode.DEGRADED
        macroSdkHome = sdk.absolutePath
        macroCompilerEnvironment = System.getenv() + mapOf("CANGJIE_HOME" to sdk.absolutePath)
        macroExpansionDemandAutoCompilationEnabled = false
        macroCompilationCacheContext = MacroCompilationCacheContext(
            targetPlatform = module.targetPlatform,
            runtimeLoaderEnvironmentFingerprint = "test:${sdk.absolutePath}",
        )
        macroConstructionReadAccess = MacroConstructionReadAccess.Direct
        roots.sourceRoots.forEach { addCangJieSourceRoot(it) }
        roots.classpathRoots.forEach { addClasspathRoot(it) }

        addClasspathRoot(macroOut.absolutePath)
        macroArtifactPackages = listOf(
            MacroArtifactPackage(
                packageFqName = FqName("demo_macro"),
                kind = MacroArtifactPackage.Kind.MACRO,
                cjoPath = File(macroOut, "demo_macro.cjo").absolutePath,
                dynamicLibPath = File(macroOut, "lib-macro_demo_macro.so").absolutePath,
                origin = MacroArtifactPackage.Origin.EXTERNAL_PATH,
            ),
        )

        macroExecutorFactory = MacroExecutorFactory {
            LspMacroServerMacroExecutor(
                macroServerPath = File(sdk, "tools/bin/LSPMacroServer").absolutePath,
                sdkHome = sdk.absolutePath,
                processEnvironment = System.getenv() + mapOf("CANGJIE_HOME" to sdk.absolutePath),
            )
        }
    }
}
