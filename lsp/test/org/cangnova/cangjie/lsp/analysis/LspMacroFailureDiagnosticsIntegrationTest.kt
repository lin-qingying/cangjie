package org.cangnova.cangjie.lsp.analysis

import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.DiagnosticCollectionFailures
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.addClasspathRoot
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.targetPlatform
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDefinitionEntry
import org.cangnova.cangjie.frontend.pipeline.FrontendMacroSemanticExpansionEngine
import org.cangnova.cangjie.frontend.pipeline.MacroCompilationCacheContext
import org.cangnova.cangjie.frontend.pipeline.MacroExecutorFactory
import org.cangnova.cangjie.frontend.pipeline.initializeCfirFrontendMacroCompilationConfiguration
import org.cangnova.cangjie.frontend.pipeline.installDefaultMacroFragmentParserFactory
import org.cangnova.cangjie.frontend.pipeline.macroCompilationCacheContext
import org.cangnova.cangjie.frontend.pipeline.macroCompilerEnvironment
import org.cangnova.cangjie.frontend.pipeline.macroConstructionMode
import org.cangnova.cangjie.frontend.pipeline.macroConstructionReadAccess
import org.cangnova.cangjie.frontend.pipeline.macroExecutorFactory
import org.cangnova.cangjie.frontend.pipeline.macroSdkHome
import org.cangnova.cangjie.lsp.CangjieLspServerOptions
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.framework.LspIntegrationTestSession
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.cangnova.cangjie.lsp.testkit.ServiceLoadedLspTestEnvironment
import com.intellij.openapi.vfs.VirtualFileManager
import org.cangnova.cangjie.macro.MacroCallInfo
import org.cangnova.cangjie.macro.MacroExecutor
import org.cangnova.cangjie.macro.MacroExpansionResult
import org.cangnova.cangjie.macro.MacroLibraryLoadResult
import org.cangnova.cangjie.macro.expansion.MacroConstructionReadAccess
import org.cangnova.cangjie.macro.expansion.MacroExpansionArtifactPreparation
import org.cangnova.cangjie.macro.expansion.MacroSemanticExpansionEngine
import org.cangnova.cangjie.macro.expansion.SdkHandle
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.eclipse.lsp4j.CodeActionContext
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.FileChangeType
import org.eclipse.lsp4j.FileEvent
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.WorkspaceDiagnosticParams
import org.eclipse.lsp4j.WorkspaceDiagnosticReport
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** 宏宿主降级、依赖源码 push diagnostics 与有效兄弟声明访问的端到端回归。 */
@Timeout(180)
class LspMacroFailureDiagnosticsIntegrationTest : AbstractLspIntegrationTest() {
    override val autoCreateDefaultSession: Boolean = false

    @Test
    fun `unavailable executor keeps declarations and publishes dependency macro diagnostics`() {
        createMacroWorkspace().use { fixture ->
            DiagnosticCollectionFailures.clearAll()
            val sdk = sdkHandle(fixture)
            val options = serverOptions(
                engine = InMemoryMacroEngine(listOf(deriveDefinition())),
                sdk = sdk,
            )
            createSession(fixture.initializeParams(), options).use { testSession ->
                val consumerUri = fixture.uri("src/Consumer.cj")
                val dependencyUri = fixture.uri("library/src/Lifecycle.cj")
                testSession.openDocument(consumerUri, fixture.text("src/Consumer.cj"))
                assertNoDiagnosticCollectionFailures("didOpen")

                val dependencyDiagnostics = awaitWorkspaceDiagnostics(testSession, dependencyUri) { diagnostics ->
                    diagnostics.any { it.code?.left == "MACRO_EXECUTOR_UNAVAILABLE" }
                }
                val macroErrors = dependencyDiagnostics.filter {
                    it.code?.left == "MACRO_EXECUTOR_UNAVAILABLE"
                }
                assertTrue(macroErrors.size >= 2, "enum 和函数上的宏调用都应报告 executor unavailable")
                assertTrue(
                    macroErrors.all { diagnostic ->
                        fixture.text("library/src/Lifecycle.cj").lines()[diagnostic.range.start.line]
                            .contains("@Derive")
                    },
                    "宏错误必须定位到未打开依赖源码中的宏调用",
                )
                val pushedDiagnostics = testSession.awaitPublishedDiagnostics(dependencyUri) { params ->
                    params.diagnostics.any { it.code?.left == "MACRO_EXECUTOR_UNAVAILABLE" }
                }
                assertTrue(pushedDiagnostics.diagnostics.isNotEmpty())
                assertNoDiagnosticCollectionFailures("macro diagnostic push")

                val consumerDiagnostics = testSession.awaitPublishedDiagnostics(consumerUri)
                val consumerLines = fixture.text("src/Consumer.cj").lines()
                assertFalse(
                    consumerDiagnostics.diagnostics.any { diagnostic ->
                        diagnostic.message.left?.contains("Unresolved reference") == true &&
                            consumerLines[diagnostic.range.start.line].let { line ->
                                line.contains("extensionProbe")
                            }
                    },
                    "保留的 enum、函数和显式 extend 成员必须能从消费模块解析",
                )
                val retainedFunctionDefinition = testSession.definition(
                    DefinitionParams(
                        TextDocumentIdentifier(consumerUri),
                        rangeOf(fixture.text("src/Consumer.cj"), "retainedFunction").start,
                    ),
                )
                assertNoDiagnosticCollectionFailures("retained function definition")
                val definitionUris = if (retainedFunctionDefinition.isLeft) {
                    retainedFunctionDefinition.left.orEmpty().map { it.uri }
                } else {
                    retainedFunctionDefinition.right.orEmpty().map { it.targetUri }
                }
                assertTrue(
                    dependencyUri in definitionUris,
                    "消费模块必须能通过 import 定位到保留的函数声明",
                )

                val callRange = rangeOf(fixture.text("src/Consumer.cj"), "extensionProbe")
                assertDoesNotThrow {
                    testSession.codeAction(
                        CodeActionParams(
                            TextDocumentIdentifier(consumerUri),
                            callRange,
                            CodeActionContext(emptyList()),
                        ),
                    )
                }
                assertNoDiagnosticCollectionFailures("codeAction")

            }
            DiagnosticCollectionFailures.clearAll()
        }
    }

    @Test
    fun `missing macro artifact reports dependency compile failure`() {
        createMacroWorkspace().use { fixture ->
            DiagnosticCollectionFailures.clearAll()
            val options = serverOptions(
                engine = FrontendMacroSemanticExpansionEngine(),
                sdk = sdkHandle(fixture),
            )
            createSession(fixture.initializeParams(), options).use { testSession ->
                testSession.openDocument(
                    fixture.uri("src/Consumer.cj"),
                    fixture.text("src/Consumer.cj"),
                )
                assertNoDiagnosticCollectionFailures("didOpen")

                val dependencyUri = fixture.uri("library/src/Lifecycle.cj")
                val dependencyDiagnostics = awaitWorkspaceDiagnostics(testSession, dependencyUri) { diagnostics ->
                    diagnostics.any { it.code?.left == "MACRO_DEPENDENCY_COMPILE_FAILED" }
                }
                assertTrue(
                    dependencyDiagnostics.any {
                        it.code?.left == "MACRO_DEPENDENCY_COMPILE_FAILED"
                    },
                    "缺失宏 artifact 且 SDK 无 cjc 时必须报告结构化编译失败",
                )
                assertTrue(
                    testSession.awaitPublishedDiagnostics(dependencyUri) { params ->
                        params.diagnostics.any { it.code?.left == "MACRO_DEPENDENCY_COMPILE_FAILED" }
                    }.diagnostics.isNotEmpty(),
                    "未打开的依赖源码 URI 也要收到宏编译诊断",
                )
                assertNoDiagnosticCollectionFailures("missing artifact diagnostics")
            }
                DiagnosticCollectionFailures.clearAll()
        }
    }

    @Test
    fun `successful expansion clears dependency diagnostics after snapshot invalidation`() {
        createMacroWorkspace().use { fixture ->
            DiagnosticCollectionFailures.clearAll()
            val executor = ToggleableIdentityMacroExecutor()
            val dependencyUri = fixture.uri("library/src/Lifecycle.cj")
            val macroSnapshotPublications = AtomicInteger()
            val options = serverOptions(
                engine = InMemoryMacroEngine(listOf(deriveDefinition())),
                sdk = sdkHandle(fixture),
                macroExecutor = executor,
                publishListener = { macroSession, _ ->
                    if ((macroSession.caModule as? CaSourceModule)?.name == "library") {
                        macroSnapshotPublications.incrementAndGet()
                    }
                },
            )
            createSession(fixture.initializeParams(), options).use { testSession ->
                val consumerUri = fixture.uri("src/Consumer.cj")
                testSession.openDocument(consumerUri, fixture.text("src/Consumer.cj"))
                assertNoDiagnosticCollectionFailures("didOpen")

                val initialDiagnostics = awaitWorkspaceDiagnostics(testSession, dependencyUri) { diagnostics ->
                    diagnostics.any { it.code?.left == "MACRO_EXECUTOR_UNAVAILABLE" }
                }
                assertTrue(initialDiagnostics.any { it.code?.left == "MACRO_EXECUTOR_UNAVAILABLE" })
                testSession.awaitPublishedDiagnostics(dependencyUri) { params ->
                    params.diagnostics.any { it.code?.left == "MACRO_EXECUTOR_UNAVAILABLE" }
                }
                assertNoDiagnosticCollectionFailures("initial macro diagnostics")

                executor.available.set(true)
                val previousPublicationCount = macroSnapshotPublications.get()
                val pushCountBeforeInvalidation =
                    testSession.publishedDiagnostics().count { it.uri == dependencyUri }
                val sourcePath = fixture.path("library/src/Lifecycle.cj")
                sourcePath.toFile().writeText(
                    fixture.text("library/src/Lifecycle.cj") + "\n// rebuild the macro snapshot\n",
                )
                VirtualFileManager.getInstance().refreshAndFindFileByNioPath(sourcePath)
                testSession.didChangeWatchedFiles(
                    DidChangeWatchedFilesParams(listOf(FileEvent(dependencyUri, FileChangeType.Changed))),
                )

                assertTrue(
                    awaitPublishedDiagnosticsAfter(
                        testSession = testSession,
                        uri = dependencyUri,
                        firstNewNotificationIndex = pushCountBeforeInvalidation,
                    ) { params -> params.diagnostics.isEmpty() }.diagnostics.isEmpty(),
                    "快照失效时必须清除旧宏诊断",
                )
                assertNoDiagnosticCollectionFailures("snapshot invalidation")
                val rebuiltDiagnostics = awaitWorkspaceDiagnostics(testSession, dependencyUri) { diagnostics ->
                    macroSnapshotPublications.get() > previousPublicationCount &&
                        executor.executedCalls.get() >= 2 &&
                        diagnostics.none { it.code?.left?.let(::isMacroCode) == true }
                }
                assertTrue(executor.executedCalls.get() >= 2, "executor must expand both declarations after invalidation")
                assertFalse(rebuiltDiagnostics.any { it.code?.left?.let(::isMacroCode) == true })
                assertNoDiagnosticCollectionFailures("successful macro expansion")
            }
        }
    }

    private fun awaitWorkspaceDiagnostics(
        testSession: LspIntegrationTestSession,
        uri: String,
        predicate: (List<Diagnostic>) -> Boolean,
    ): List<Diagnostic> {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        var lastDiagnostics: List<Diagnostic>? = null
        while (System.nanoTime() < deadline) {
            val report = testSession.workspaceDiagnostic(WorkspaceDiagnosticParams())
            assertNoDiagnosticCollectionFailures("workspace diagnostic request")
            lastDiagnostics = report.items.firstNotNullOfOrNull { item ->
                if (!item.isWorkspaceFullDocumentDiagnosticReport) return@firstNotNullOfOrNull null
                item.workspaceFullDocumentDiagnosticReport
                    .takeIf { it.uri == uri }
                    ?.items
            }
            val diagnostics = lastDiagnostics
            if (diagnostics != null && predicate(diagnostics)) return diagnostics
            Thread.sleep(100)
        }
        error("Timed out waiting for workspace diagnostics for $uri, last=$lastDiagnostics")
    }

    private fun awaitPublishedDiagnosticsAfter(
        testSession: LspIntegrationTestSession,
        uri: String,
        firstNewNotificationIndex: Int,
        predicate: (PublishDiagnosticsParams) -> Boolean,
    ): PublishDiagnosticsParams {
        check(firstNewNotificationIndex >= 0) { "Macro snapshot publication was not observed for $uri" }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        var lastNotification: PublishDiagnosticsParams? = null
        while (System.nanoTime() < deadline) {
            val notifications = testSession.publishedDiagnostics().filter { it.uri == uri }
            lastNotification = notifications.drop(firstNewNotificationIndex).firstOrNull(predicate)
            if (lastNotification != null) return lastNotification
            Thread.sleep(20)
        }
        error("Timed out waiting for post-publication diagnostics for $uri, last=$lastNotification")
    }

    private fun assertNoDiagnosticCollectionFailures(operation: String) {
        val failures = DiagnosticCollectionFailures.drain()
        assertTrue(
            failures.isEmpty(),
            "$operation isolated ${failures.size} diagnostic exception(s):\n" +
                failures.joinToString("\n") { it.stackTraceToString() },
        )
    }

    private fun createMacroWorkspace(): LspWorkspaceFixture =
        LspWorkspaceFixtureBuilder()
            .addModule(
                name = "consumer",
                rootRelativePath = "",
                requiredModuleNames = listOf("library"),
            )
            .addModule(
                name = "library",
                rootRelativePath = "library",
                requiredModuleNames = listOf("derive-api"),
            )
            .addModule(name = "derive-api", rootRelativePath = "derive-api")
            .file(
                "derive-api/src/Derive.cj",
                """
                    macro package derive_api
                    import std.ast.*

                    public macro Derive(input: Tokens): Tokens {
                        return input
                    }
                """.trimIndent(),
            )
            .file(
                "library/src/Lifecycle.cj",
                """
                    package model
                    import derive_api.Derive

                    @Derive
                    public enum Lifecycle {
                        | Ready | Done
                    }

                    @Derive
                    public func retainedFunction(): Int64 {
                        return 7
                    }

                    extend Lifecycle {
                        public func extensionProbe(): Int64 {
                            return 1
                        }
                    }
                """.trimIndent(),
            )
            .source(
                "Consumer.cj",
                """
                    package consumer
                    import model.Lifecycle
                    import model.retainedFunction
                    public func useOriginalDeclarations(value: Lifecycle): Int64 {
                        return value.extensionProbe()
                    }
                """.trimIndent(),
            )
            .build()

    private fun sdkHandle(fixture: LspWorkspaceFixture): SdkHandle {
        val home = fixture.workspaceRoot.resolve(".macro-sdk-without-cjc").toString()
        return SdkHandle(
            id = "portable-macro-test-sdk",
            homePath = home,
            environment = mapOf("CANGJIE_HOME" to home),
            stdlibPath = fixture.stdlibRoot?.toString(),
        )
    }

    @OptIn(CompilerConfiguration.Internals::class)
    private fun serverOptions(
        engine: MacroSemanticExpansionEngine,
        sdk: SdkHandle,
        macroExecutor: MacroExecutor? = null,
        publishListener: (LLCfirResolvableModuleSession, Set<VirtualFile>) -> Unit = { _, _ -> },
    ): CangjieLspServerOptions {
        val macroOptions = LspMacroExpansionOptions(
            engine = engine,
            sdkProvider = { sdk },
            compilerConfigurationProvider = { module: CaSourceModule, roots, _ ->
                CompilerConfiguration().apply {
                    languageVersionSettings = module.languageVersionSettings
                    targetPlatform = module.targetPlatform
                    moduleName = module.name
                    useLightTree = false
                    initializeCfirFrontendMacroCompilationConfiguration()
                    macroConstructionMode = MacroConstructionService.Mode.DEGRADED
                    macroSdkHome = sdk.homePath
                    macroCompilerEnvironment = sdk.environment
                    macroCompilationCacheContext = MacroCompilationCacheContext(
                        targetPlatform = module.targetPlatform,
                        runtimeLoaderEnvironmentFingerprint = "${sdk.id}@${sdk.homePath}",
                    )
                    macroConstructionReadAccess = MacroConstructionReadAccess.Direct
                    roots.sourceRoots.forEach { addCangJieSourceRoot(it) }
                    roots.classpathRoots.forEach { addClasspathRoot(it) }
                    sdk.stdlibPath?.let { addClasspathRoot(it) }
                    if (macroExecutor != null) {
                        macroExecutorFactory = MacroExecutorFactory { macroExecutor }
                    }
                }
            },
            fragmentParserInstaller = { module, configuration ->
                configuration.installDefaultMacroFragmentParserFactory(module.project)
            },
            externalMacroPackagesProvider = { setOf(FqName("derive_api")) },
            publishListener = publishListener,
        )
        return CangjieLspServerOptions(
            environmentMode = CangJieCoreEnvironmentMode.UnitTest,
            macroExpansion = macroOptions,
            environmentFactory = {
                ServiceLoadedLspTestEnvironment.create(
                    mode = CangJieCoreEnvironmentMode.UnitTest,
                    macroExpansion = macroOptions,
                )
            },
            exitHandler = {},
        )
    }

    private fun deriveDefinition() = MacroDefinitionEntry(
        packageFqName = FqName("derive_api"),
        name = Name.identifier("Derive"),
        source = MacroDefinitionEntry.Source.MACRO_ARTIFACT,
        libPath = "memory:derive",
    )

    private fun rangeOf(text: String, token: String): Range {
        val lineIndex = text.lines().indexOfFirst { token in it }
        check(lineIndex >= 0) { "Cannot find `$token` in the fixture source" }
        val line = text.lines()[lineIndex]
        val start = line.indexOf(token)
        return Range(Position(lineIndex, start), Position(lineIndex, start + token.length))
    }

    private class InMemoryMacroEngine(
        private val definitions: List<MacroDefinitionEntry>,
    ) : MacroSemanticExpansionEngine by FrontendMacroSemanticExpansionEngine() {
        override fun prepareArtifacts(
            configuration: CompilerConfiguration,
            classifications: List<org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDemandClassification>,
        ): MacroExpansionArtifactPreparation = MacroExpansionArtifactPreparation(definitions = definitions)
    }

    private class ToggleableIdentityMacroExecutor : MacroExecutor {
        val available = AtomicBoolean(false)
        val executedCalls = AtomicInteger()

        override fun loadLibraries(libPaths: List<String>): MacroLibraryLoadResult =
            MacroLibraryLoadResult.Success(libPaths)

        override fun execute(calls: List<MacroCallInfo>): List<MacroExpansionResult> {
            executedCalls.addAndGet(calls.size)
            return calls.map { call ->
                MacroExpansionResult.Success(
                    tokens = call.argTokens,
                    expandedText = call.argTokens.joinToString(separator = "") { it.value },
                )
            }
        }

        override fun reset() = Unit

        override fun isAvailable(): Boolean = available.get()

        override fun close() = Unit
    }
}

private fun isMacroCode(code: String): Boolean =
    code.startsWith("MACRO_") || code.startsWith("CFIR_MACRO_")
