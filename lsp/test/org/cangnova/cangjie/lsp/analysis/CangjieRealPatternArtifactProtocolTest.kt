package org.cangnova.cangjie.lsp.analysis

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.restoreSymbol
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.api.symbols.*
import org.cangnova.cangjie.analysis.decompiled.psi.CjoFileDecompilers
import org.cangnova.cangjie.analysis.decompiler.stub.file.CangJieMetadataStubBuilder
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.DiagnosticCollectionFailures
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.analysis.macro.expansion.MacroSemanticExpansionService
import org.cangnova.cangjie.config.*
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.frontend.pipeline.*
import org.cangnova.cangjie.lsp.CangjieLspEnvironment
import org.cangnova.cangjie.lsp.CangjieLspServerOptions
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.framework.LspIntegrationTestSession
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.*
import org.cangnova.cangjie.macro.expansion.MacroConstructionReadAccess
import org.cangnova.cangjie.macro.expansion.MacroExpansionOutcome
import org.cangnova.cangjie.macro.expansion.SdkHandle
import org.cangnova.cangjie.macro.process.LspMacroServerMacroExecutor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.eclipse.lsp4j.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

/** 官方 cjc/CJO/DLL 与真实 LSP 双端连接共同验证；SDK 缺失或 native 失败必须失败。 */
@EnabledOnOs(OS.WINDOWS)
@Timeout(180)
class CangjieRealPatternArtifactProtocolTest : AbstractLspIntegrationTest() {
    override val autoCreateDefaultSession: Boolean = false
    private lateinit var environment: CangjieLspEnvironment
    private var macroOptions: LspMacroExpansionOptions? = null

    /** 只记录真实 construction 回调；同一个 session 的每次 started 都有独立观测对象。 */
    private class NativeConstruction(val session: LLCfirResolvableModuleSession) {
        @Volatile
        var outcome: MacroExpansionOutcome? = null
    }

    private val nativeConstructions = CopyOnWriteArrayList<NativeConstruction>()
    private val nativeTerminalEvents =
        CopyOnWriteArrayList<Pair<LLCfirResolvableModuleSession, MacroExpansionOutcome>>()
    private val severeLogs = CopyOnWriteArrayList<LogRecord>()
    private val severeHandler = object : Handler() {
        override fun publish(record: LogRecord) {
            if (record.level.intValue() >= Level.SEVERE.intValue()) severeLogs.add(record)
        }

        override fun flush() {}
        override fun close() {}
    }

    /** 保留原日志输出，同时让异步刷新/关闭阶段的 SEVERE 成为真实失败。 */
    @BeforeEach
    fun observeNativeProtocolFailures() {
        Logger.getLogger("").addHandler(severeHandler)
    }

    @AfterEach
    @Timeout(30)
    fun assertNativeProtocolFailuresAreNotIgnored() {
        try {
            assertAll(
                { assertNoNativeConstructionFailures() },
                {
                    nativeConstructions.forEach { construction ->
                        assertNotNull(construction.outcome,
                            "真实 native started 没有终态回调，不能把 idle 当成功：${construction.session}")
                    }
                },
                {
                    val failures = DiagnosticCollectionFailures.drain()
                    assertEquals(emptyList<Throwable>(), failures,
                        "真实 LL 诊断隔离异常不能作为零 error：$failures")
                },
                {
                    assertEquals(emptyList<LogRecord>(), severeLogs.toList(),
                        "真实协议不能忽略 SEVERE：${severeLogs.map { "${it.loggerName}: ${it.message} ${it.thrown}" }}")
                },
            )
        } finally {
            Logger.getLogger("").removeHandler(severeHandler)
        }
    }

    override fun defaultServerOptions(): CangjieLspServerOptions = CangjieLspServerOptions(
        environmentMode = CangJieCoreEnvironmentMode.UnitTest,
        macroExpansion = macroOptions,
        environmentFactory = {
            ServiceLoadedLspTestEnvironment.create(macroExpansion = macroOptions).also { environment = it }
        },
        exitHandler = {},
    )

    /** 库源码不进入 LSP source roots；声明 oracle 来自实际 cjc CJO 的独立 compiled PSI。 */
    @Test
    fun `real CJO tuple leaves preserve aliases and includeDeclaration`(@TempDir root: Path) {
        val sdk = CangjieSdkFixture.requireSdkHome("真实 CJO pattern LSP 协议")
        val out = Files.createDirectories(root.resolve("binary"))
        official(sdk, out, "-p", LspTestData.path("realSemantics/patternArtifacts/library").toString(),
            "--output-type=staticlib", "--output-dir", out.toString(), "-o", "libbinarypatterns.a")
        val cjo = out.resolve("sample.binarypatterns.cjo")
        assertTrue(Files.isRegularFile(cjo), "必须使用官方编译产物")
        official(sdk, root, "-p", LspTestData.path("realSemantics/patternArtifacts/binary-consumer").toString(),
            "--import-path", out.toString(), "--output-type=staticlib", "--output-dir", root.toString(),
            "-o", "libconsumer.a")
        LspWorkspaceFixtureBuilder()
            .addModule(name = "consumer", packageSearchPaths = listOf(out.toString()))
            .sourceFromTestData("Consumer.cj", "realSemantics/patternArtifacts/binary-consumer/Consumer.cj")
            .build().use { fixture ->
                // 全新 server/project 重复同一 oracle；不复用旧 session 的 symbol。
                repeat(2) {
                    createSession(fixture.initializeParams()).use { session ->
                        session.openDocument(fixture.uri("src/Consumer.cj"), fixture.text("src/Consumer.cj"))
                        assertNoErrors(session, fixture, "src/Consumer.cj")
                        val vf = requireNotNull(VirtualFileManager.getInstance().refreshAndFindFileByNioPath(cjo))
                        val manager = PsiManager.getInstance(environment.project)
                        val psi = manager.findFile(vf)
                        val compiled = requireNotNull(psi as? CjFile) {
                            val full = CjoFileDecompilers.find(vf, CjoFileDecompilers.Full::class.java)
                            val builder = full?.getStubBuilder() as? CangJieMetadataStubBuilder
                            val header = vf.inputStream.use { it.readNBytes(16) }.joinToString(" ") { "%02x".format(it.toInt() and 0xff) }
                            "真实 CJO PSI 缺失：type=${vf.fileType.name}/${vf.fileType.javaClass.name}; " +
                                "binary=${vf.fileType.isBinary}; extension=${vf.extension}; header=$header; " +
                                "projectDefault=${environment.project.isDefault}; psi=${psi?.javaClass?.name}; " +
                                "view=${manager.findViewProvider(vf)?.javaClass?.name}; " +
                                "registeredFull=${CjoFileDecompilers.EP_NAME.extensionList.map { it.javaClass.name }}; " +
                                "acceptedFull=${full?.javaClass?.name}; supported=${builder?.isSupported(vf)}; " +
                                "hasStub=${builder?.hasStub(vf)}; metadata=${builder?.readFileSafely(vf, project = environment.project)?.javaClass?.name}"
                        }
                        assertTrue(compiled.isCompiled)
                        // 真实 compiled stub 是声明树来源；此处不能用强制加载反编译 AST 的遍历入口。
                        val pending = ArrayDeque<com.intellij.psi.stubs.StubElement<*>>()
                        pending.add(requireNotNull(compiled.stub))
                        val bindings = buildList {
                            while (pending.isNotEmpty()) {
                                val stub = pending.removeFirst()
                                (stub.psi as? CjBindingPattern)?.let(::add)
                                pending.addAll(stub.childrenStubs)
                            }
                        }.associateBy { it.name }
                        assertEquals(setOf("libraryCount", "libraryLabel", "libraryFlag"), bindings.keys)
                        val uses = binaryUses(fixture)
                        val source = currentFile(fixture, "src/Consumer.cj")
                        readAnalyze(source) {
                            for (name in uses.keys) {
                                val declaration = bindings.getValue(name)
                                val leaf = requireNotNull(declaration.bindingSymbol)
                                assertInstanceOf(CaPatternBindingSymbol::class.java, leaf)
                                val owner = requireNotNull(declaration.getStrictParentOfType<CjPatternVariable>()).symbol
                                assertEquals(listOf(leaf), owner.entries)
                                val module = assertInstanceOf(CaLibraryModule::class.java, leaf.containingModule)
                                assertNotEquals(useSiteModule, module)
                                assertEquals(CaSymbolOrigin.LIBRARY, leaf.origin)
                                assertSame(declaration, leaf.psi)
                                assertEquals(FqName("sample.binarypatterns"), requireNotNull(leaf.callableId).packageName)
                                assertEquals(leaf, getTopLevelCallableSymbols(FqName("sample.binarypatterns"), Name.identifier(name)).single())
                                val alias = when (name) {
                                    "libraryCount" -> "firstCount"
                                    else -> name
                                }
                                val use = source.collectDescendantsOfType<CjSimpleNameExpression>()
                                    .last { it.text == alias }
                                assertEquals(leaf, use.resolveToSymbol())
                                assertEquals(name, leaf.name.asString())
                            }
                        }
                        for ((name, expectedUses) in uses) {
                            val declaration = bindings.getValue(name)
                            val libraryDocument = LspTextDocument(cjo.toUri().toString(), "cangjie", 0, compiled.text)
                            val nameRange = requireNotNull(declaration.nameIdentifier).textRange
                            val declarationLocation = Location(cjo.toUri().toString(),
                                Range(libraryDocument.positionAt(nameRange.startOffset), libraryDocument.positionAt(nameRange.endOffset)))
                            val definition = Location(cjo.toUri().toString(), Range(libraryDocument.positionAt(declaration.textRange.startOffset),
                                libraryDocument.positionAt(declaration.textRange.endOffset)))
                            assertProtocol(session, expectedUses, declarationLocation, definition)
                        }
                    }
                }
            }
    }

    /** 三个生成叶子共享 producing invocation，但跨包别名引用集合必须各自独立。 */
    @Test
    fun `real native macro tuple leaves preserve canonical identities`(@TempDir root: Path) {
        val sdk = CangjieSdkFixture.requireSdkHome("真实 native macro pattern LSP 协议")
        CangjieSdkFixture.requireMacroServer(sdk)
        val out = Files.createDirectories(root.resolve("macros"))
        official(sdk, out, "-p", LspTestData.path("realSemantics/patternArtifacts/macros").toString(),
            "--compile-macro", "--output-dir", out.toString())
        assertTrue(Files.isRegularFile(out.resolve("patternmacros.cjo")))
        assertTrue(Files.isRegularFile(out.resolve("lib-macro_patternmacros.dll")))
        val hostOut = Files.createDirectories(root.resolve("host"))
        official(sdk, hostOut, "-p", LspTestData.path("realSemantics/patternArtifacts/macro-host").toString(),
            "--import-path", out.toString(), "--output-type=staticlib", "--output-dir", hostOut.toString(),
            "-o", "libhost.a")
        official(sdk, root, "-p", LspTestData.path("realSemantics/patternArtifacts/macro-consumer").toString(),
            "--import-path", hostOut.toString(), "--output-type=staticlib", "--output-dir", root.toString(),
            "-o", "libconsumer.a")
        macroOptions = nativeMacroOptions(sdk, out)
        LspWorkspaceFixtureBuilder()
            .addModule(name = "provider", rootRelativePath = "provider", packageSearchPaths = listOf(out.toString()))
            .addModule(name = "consumer", rootRelativePath = "consumer", requiredModuleNames = listOf("provider"))
            .fileFromTestData("provider/src/Host.cj", "realSemantics/patternArtifacts/macro-host/Host.cj")
            .fileFromTestData("consumer/src/Consumer.cj", "realSemantics/patternArtifacts/macro-consumer/Consumer.cj")
            .build().use { fixture ->
                repeat(2) {
                    createSession(fixture.initializeParams()).use { session ->
                        try {
                            listOf("provider/src/Host.cj", "consumer/src/Consumer.cj").forEach { path ->
                                session.openDocument(fixture.uri(path), fixture.text(path))
                            }
                            val expectedDocuments = listOf("provider/src/Host.cj", "consumer/src/Consumer.cj")
                                .associateWith { 1 to fixture.text(it) }
                            awaitServerDocuments(session, fixture, expectedDocuments)
                            // 从真实通知完成后的当前模块构造；awaitIdle 不能代替 PUBLISHED。
                            val structure = AnalysisApiLspProjectStructureState.getInstance(environment.project)
                            val module = structure.allSourceLikeModules.filterIsInstance<CaSourceModule>().single { it.name == "provider" }
                            val ll = ReadAction.compute<LLCfirResolvableModuleSession, RuntimeException> {
                                module.getResolutionFacade(environment.project).sessionProvider.getResolvableSession(module)
                            }
                            val expansion = MacroSemanticExpansionService.getInstance(environment.project)
                            assertCurrentNativeOwner(fixture, "provider/src/Host.cj", module, ll)
                            expansion.request(ll)
                            expansion.awaitIdle(module)
                            assertPublishedNativeSnapshot(fixture, "provider/src/Host.cj", module, ll, expansion)
                            assertProviderExportsReachConsumer(fixture, "provider/src/Host.cj", "consumer/src/Consumer.cj", module)
                            assertFreshNativeDiagnostics(session, fixture, expectedDocuments)
                            assertNotNull(ll.macroSemanticSnapshot, "native 宏必须发布当前 snapshot")
                            assertNoErrors(session, fixture, "provider/src/Host.cj")
                            assertNoErrors(session, fixture, "consumer/src/Consumer.cj")
                            val producer = location(fixture, "provider/src/Host.cj",
                                "@EmitGlobals\npublic func carrier(): Unit {}", "@EmitGlobals\npublic func carrier(): Unit {}")
                            val uses = macroUses(fixture)
                            val host = currentFile(fixture, "provider/src/Host.cj")
                            val producingPsi = ReadAction.compute<CjMacroExpression, RuntimeException> {
                                host.collectDescendantsOfType<CjMacroExpression>().single { it.text.startsWith("@EmitGlobals") }
                            }
                            readAnalyze(host) {
                                val leaves = uses.keys.map { name ->
                                    val leaf = assertInstanceOf(CaPatternBindingSymbol::class.java,
                                        getTopLevelCallableSymbols(FqName("sample.patternmacro"), Name.identifier(name)).single())
                                    assertSame(module, leaf.containingModule)
                                    assertEquals(FqName("sample.patternmacro"), requireNotNull(leaf.callableId).packageName)
                                    assertNotNull(leaf.psi)
                                    assertNotSame(producingPsi, leaf.psi, "规范生成叶子不能变成共同 producing invocation")
                                    leaf
                                }
                                assertEquals(3, leaves.toSet().size, "共同 producing invocation 不得合并叶子身份")
                                assertEquals(3, leaves.map { it.psi }.toSet().size, "每个 generated binder 必须拥有独立当前 PSI")
                            }
                            uses.values.forEach { assertProtocol(session, it, producer, producer) }
                        } finally {
                            awaitStartedNativeConstructions()
                        }
                    }
                }
            }
    }

    /**
     * 在同一个真实 server 中更新宿主，旧 source/library 会话退役后重新展开 DLL。
     * 全部 declaration/use/alias/includeDeclaration oracle 保持不变，pointer 只在 fresh analyze 中恢复。
     */
    @Test
    fun `native macro targets restore after host version change`(@TempDir root: Path) {
        val sdk = CangjieSdkFixture.requireSdkHome("native macro current-generation LSP 协议")
        CangjieSdkFixture.requireMacroServer(sdk)
        val out = Files.createDirectories(root.resolve("macros"))
        official(sdk, out, "-p", LspTestData.path("realSemantics/patternArtifacts/macros").toString(),
            "--compile-macro", "--output-dir", out.toString())
        assertTrue(Files.isRegularFile(out.resolve("patternmacros.cjo")))
        assertTrue(Files.isRegularFile(out.resolve("lib-macro_patternmacros.dll")))
        macroOptions = nativeMacroOptions(sdk, out)
        LspWorkspaceFixtureBuilder()
            .addModule(name = "provider", rootRelativePath = "provider", packageSearchPaths = listOf(out.toString()))
            .addModule(name = "consumer", rootRelativePath = "consumer", requiredModuleNames = listOf("provider"))
            .fileFromTestData("provider/src/Host.cj", "realSemantics/patternArtifacts/macro-host/Host.cj")
            .fileFromTestData("consumer/src/Consumer.cj", "realSemantics/patternArtifacts/macro-consumer/Consumer.cj")
            .build().use { fixture ->
                createSession(fixture.initializeParams()).use { session ->
                    try {
                        val hostPath = "provider/src/Host.cj"
                        val consumerPath = "consumer/src/Consumer.cj"
                        listOf(hostPath, consumerPath).forEach { path ->
                            session.openDocument(fixture.uri(path), fixture.text(path))
                        }
                        val expectedDocuments = listOf(hostPath, consumerPath).associateWith { 1 to fixture.text(it) }
                        awaitServerDocuments(session, fixture, expectedDocuments)
                        val structure = AnalysisApiLspProjectStructureState.getInstance(environment.project)
                        val expansion = MacroSemanticExpansionService.getInstance(environment.project)
                        val oldModule = structure.allSourceLikeModules.filterIsInstance<CaSourceModule>().single { it.name == "provider" }
                        val oldSession = ReadAction.compute<LLCfirResolvableModuleSession, RuntimeException> {
                                oldModule.getResolutionFacade(environment.project).sessionProvider.getResolvableSession(oldModule)
                            }
                        assertCurrentNativeOwner(fixture, hostPath, oldModule, oldSession)
                        expansion.request(oldSession)
                        expansion.awaitIdle(oldModule)
                        assertPublishedNativeSnapshot(fixture, hostPath, oldModule, oldSession, expansion)
                        assertProviderExportsReachConsumer(fixture, hostPath, consumerPath, oldModule)
                        assertFreshNativeDiagnostics(session, fixture, expectedDocuments)
                        val oldSnapshot = requireNotNull(oldSession.macroSemanticSnapshot)
                        val uses = macroUses(fixture)
                        val producer = location(fixture, hostPath,
                            "@EmitGlobals\npublic func carrier(): Unit {}", "@EmitGlobals\npublic func carrier(): Unit {}")
                        val pointers = readAnalyze(currentFile(fixture, hostPath)) {
                            uses.keys.associateWith { name ->
                                val symbol = assertInstanceOf(CaPatternBindingSymbol::class.java,
                                    getTopLevelCallableSymbols(FqName("sample.patternmacro"), Name.identifier(name)).single())
                                assertSame(oldModule, symbol.containingModule)
                                symbol.createPointer()
                            }
                        }
                        uses.values.forEach { assertProtocol(session, it, producer, producer) }
                        val updatedHost = fixture.text(hostPath).replace("hostAnchor: Int64 = 40", "hostAnchor: Int64 = 41")
                        assertNotEquals(fixture.text(hostPath), updatedHost)
                        assertEquals(fixture.text(hostPath).length, updatedHost.length)
                        session.changeDocument(fixture.uri(hostPath), updatedHost, 2)
                        // 等待真实服务端已消费版本通知，不过滤任何 error/severe 诊断。
                        session.awaitPublishedDiagnostics(fixture.uri(hostPath)) { it.version == 2 }
                        assertServerDocuments(fixture,
                            mapOf(hostPath to (2 to updatedHost), consumerPath to (1 to fixture.text(consumerPath))))
                        val freshModule = structure.allSourceLikeModules.filterIsInstance<CaSourceModule>().single { it.name == "provider" }
                        assertNotSame(oldModule, freshModule)
                        assertFalse(oldSession.isValid, "旧 module/session 必须退役，不能复用旧宏树")
                        val freshSession = ReadAction.compute<LLCfirResolvableModuleSession, RuntimeException> {
                                freshModule.getResolutionFacade(environment.project).sessionProvider.getResolvableSession(freshModule)
                            }
                        assertNotSame(oldSession, freshSession)
                        assertCurrentNativeOwner(fixture, hostPath, freshModule, freshSession)
                        expansion.request(freshSession)
                        expansion.awaitIdle(freshModule)
                        assertPublishedNativeSnapshot(fixture, hostPath, freshModule, freshSession, expansion)
                        assertProviderExportsReachConsumer(fixture, hostPath, consumerPath, freshModule)
                        val freshSnapshot = requireNotNull(freshSession.macroSemanticSnapshot)
                        assertNotSame(oldSnapshot, freshSnapshot)
                        assertEquals(updatedHost, ReadAction.compute<String, RuntimeException> { currentFile(fixture, hostPath).text })
                        readAnalyze(currentFile(fixture, hostPath)) {
                            for ((name, pointer) in pointers) {
                                val restored = assertInstanceOf(CaPatternBindingSymbol::class.java, restoreSymbol(pointer))
                                assertSame(freshModule, restored.containingModule)
                                assertEquals(restored, getTopLevelCallableSymbols(FqName("sample.patternmacro"), Name.identifier(name)).single())
                            }
                        }
                        // 当前 pull 与回调旧 push 分开：只验 fresh generation 的实际诊断，不把 push 时序当语义结论。
                        listOf(hostPath, consumerPath).forEach { path ->
                            val report = session.documentDiagnostic(session.documentDiagnosticParams(fixture.uri(path)))
                            assertTrue(report.isRelatedFullDocumentDiagnosticReport)
                            assertEquals(emptyList<Diagnostic>(), report.relatedFullDocumentDiagnosticReport.items
                                .filter { it.severity == DiagnosticSeverity.Error })
                        }
                        uses.values.forEach { assertProtocol(session, it, producer, producer) }
                    } finally {
                        awaitStartedNativeConstructions()
                    }
                }
            }
    }

    /**
     * didOpen/didChange 只发送通知；以真实 versioned push 确认服务端已消费指定版本。
     * predicate 只判断版本，失败诊断保持原记录；零 error 由后续真实 pull/原 push 断言验证。
     */
    private fun awaitServerDocuments(
        session: LspIntegrationTestSession,
        fixture: LspWorkspaceFixture,
        expected: Map<String, Pair<Int, String>>,
    ) {
        expected.forEach { (path, value) ->
            session.awaitPublishedDiagnostics(fixture.uri(path)) { it.version == value.first }
        }
        assertServerDocuments(fixture, expected)
    }

    /** 当前 document、source graph 和 PSI 登记必须表示同一个真实 generation。 */
    private fun assertServerDocuments(
        fixture: LspWorkspaceFixture,
        expected: Map<String, Pair<Int, String>>,
    ) = ReadAction.run<RuntimeException> {
        val structure = AnalysisApiLspProjectStructureState.getInstance(environment.project)
        expected.forEach { (path, value) ->
            val uri = fixture.uri(path)
            val opened = requireNotNull(structure.openDocumentSnapshot(uri))
            assertEquals(value.first, opened.document.version)
            assertEquals(value.second, opened.document.text)
            assertEquals(value.second.replace("\r\n", "\n"), opened.psiFile.text)
            assertSame(opened.psiFile, currentFile(fixture, path),
                "通知后的 PSI 必须是当前 source graph 登记对象")
            val module = assertInstanceOf(CaSourceModule::class.java,
                requireNotNull(structure.useSiteModuleForOpenDocument(uri)))
            assertSame(module, structure.getModuleForFile(opened.psiFile))
        }
    }

    /** fixture 名称只用于选择模块；owner 来自文档、PSI 和真实 requested session 的实际绑定。 */
    private fun assertCurrentNativeOwner(
        fixture: LspWorkspaceFixture,
        hostPath: String,
        module: CaSourceModule,
        nativeSession: LLCfirResolvableModuleSession,
    ) = ReadAction.run<RuntimeException> {
        val structure = AnalysisApiLspProjectStructureState.getInstance(environment.project)
        assertSame(module, structure.useSiteModuleForOpenDocument(fixture.uri(hostPath)))
        assertSame(module, structure.getModuleForFile(currentFile(fixture, hostPath)))
        assertSame(module, nativeSession.caModule)
        assertTrue(nativeSession.isValid, "当前 native 构造不能使用已退役 session")
        assertNoNativeConstructionFailures()
    }

    /** idle 只证明在途任务结束；PUBLISHED 必须同时来自实际 session 回调、metrics 和当前 source snapshot。 */
    private fun assertPublishedNativeSnapshot(
        fixture: LspWorkspaceFixture,
        hostPath: String,
        module: CaSourceModule,
        nativeSession: LLCfirResolvableModuleSession,
        expansion: MacroSemanticExpansionService,
    ) {
        assertCurrentNativeOwner(fixture, hostPath, module, nativeSession)
        assertEquals(MacroExpansionOutcome.PUBLISHED,
            nativeTerminalEvents.lastOrNull { it.first === nativeSession }?.second,
            "requested session 没有真实 PUBLISHED 回调")
        val metrics = expansion.metrics(module)
        assertEquals(MacroExpansionOutcome.PUBLISHED, metrics.lastOutcome, "当前 native 构造未成功：$metrics")
        assertTrue(metrics.published > 0)
        val snapshot = requireNotNull(nativeSession.macroSemanticSnapshot) {
            "当前 native session 未发布真实 snapshot：$metrics"
        }
        assertNotNull(snapshot.fileForSource(currentFile(fixture, hostPath)),
            "snapshot 必须包含当前 Host PSI generation")
    }

    /** 先证明真实 provider 导出了逐叶符号，再证明 consumer 当前依赖视图能恢复同一身份。 */
    private fun assertProviderExportsReachConsumer(
        fixture: LspWorkspaceFixture,
        hostPath: String,
        consumerPath: String,
        providerModule: CaSourceModule,
    ) = ReadAction.run<RuntimeException> {
        val structure = AnalysisApiLspProjectStructureState.getInstance(environment.project)
        val consumerModule = assertInstanceOf(CaSourceModule::class.java,
            requireNotNull(structure.useSiteModuleForOpenDocument(fixture.uri(consumerPath))))
        assertTrue(consumerModule.directRegularDependencies.any { it === providerModule })
        val pointers = readAnalyze(currentFile(fixture, hostPath)) {
            macroUses(fixture).keys.associateWith { name ->
                val symbol = assertInstanceOf(CaPatternBindingSymbol::class.java,
                    getTopLevelCallableSymbols(FqName("sample.patternmacro"), Name.identifier(name)).single())
                assertSame(providerModule, symbol.containingModule)
                assertNotNull(symbol.psi)
                symbol.createPointer()
            }
        }
        readAnalyze(currentFile(fixture, consumerPath)) {
            assertSame(consumerModule, useSiteModule)
            pointers.forEach { (name, pointer) ->
                val restored = assertInstanceOf(CaPatternBindingSymbol::class.java, restoreSymbol(pointer))
                val imported = assertInstanceOf(CaPatternBindingSymbol::class.java,
                    getTopLevelCallableSymbols(FqName("sample.patternmacro"), Name.identifier(name)).single())
                assertSame(providerModule, imported.containingModule)
                assertEquals(restored, imported, "consumer 依赖 session 必须读取实际 provider 的当前生成叶子")
            }
        }
    }

    /** 成功 snapshot 后直接查询当前完整诊断；不通过挑选无 Error 的 push 等待成功。 */
    private fun assertFreshNativeDiagnostics(
        session: LspIntegrationTestSession,
        fixture: LspWorkspaceFixture,
        expected: Map<String, Pair<Int, String>>,
    ) {
        expected.keys.forEach { path ->
            val report = session.documentDiagnostic(session.documentDiagnosticParams(fixture.uri(path)))
            assertTrue(report.isRelatedFullDocumentDiagnosticReport)
            assertEquals(emptyList<Diagnostic>(), report.relatedFullDocumentDiagnosticReport.items
                .filter { it.severity == DiagnosticSeverity.Error })
        }
        assertServerDocuments(fixture, expected)
        assertNoNativeConstructionFailures()
    }

    private fun assertNoNativeConstructionFailures() {
        assertTrue(nativeTerminalEvents.none { it.second == MacroExpansionOutcome.FAILED },
            "真实 native FAILED 不能被后续发布/退役覆盖：$nativeTerminalEvents")
    }

    /** 排队任务可能在 started 之前取消；所有真实终态事件都保留，不能只保存最近成功结果。 */
    private fun recordNativeOutcome(session: LLCfirResolvableModuleSession, outcome: MacroExpansionOutcome) {
        nativeTerminalEvents.add(session to outcome)
        nativeConstructions.lastOrNull { it.session === session && it.outcome == null }?.outcome = outcome
    }

    /** 在 use 关闭连接之前等待本工程已观测 started 的实际构造，不吞取消/中断或把超时算作完成。 */
    private fun awaitStartedNativeConstructions() {
        val expansion = MacroSemanticExpansionService.getInstance(environment.project)
        var index = 0
        while (index < nativeConstructions.size) {
            val construction = nativeConstructions[index++]
            if (construction.session.project === environment.project) {
                expansion.awaitIdle(assertInstanceOf(CaSourceModule::class.java, construction.session.caModule))
            }
        }
    }

    /** 独立 fixture 范围 oracle；所有用法作为查询入口，true/false 差集恰为一个声明导航位置。 */
    private fun assertProtocol(session: LspIntegrationTestSession, uses: List<Location>, declaration: Location, definition: Location) {
        assertTrue(uses.isNotEmpty())
        for (query in uses) {
            for (includeDeclaration in listOf(false, true)) {
                val expected = if (includeDeclaration) uses + declaration else uses
                val actual = session.references(ReferenceParams(TextDocumentIdentifier(query.uri), query.range.start, ReferenceContext(includeDeclaration)))
                assertEquals(expected.size, actual.size, "query=$query includeDeclaration=$includeDeclaration")
                assertEquals(expected.toSet(), actual.toSet(), "精确范围、全部别名、sibling/wildcard 隔离")
            }
            val actual = session.definition(DefinitionParams(TextDocumentIdentifier(query.uri), query.range.start))
            val locations = if (actual.isLeft) actual.left else actual.right.map { Location(it.targetUri, it.targetRange) }
            assertEquals(listOf(definition), locations, "definition 与 reference 声明位置来自同一 canonical leaf")
        }
    }

    private fun binaryUses(f: LspWorkspaceFixture): Map<String, List<Location>> = linkedMapOf(
        "libraryCount" to listOf(
            location(f, "src/Consumer.cj", "libraryCount as firstCount", "libraryCount"),
            location(f, "src/Consumer.cj", "libraryCount as secondCount", "libraryCount"),
            location(f, "src/Consumer.cj", "return firstCount +", "firstCount"),
            location(f, "src/Consumer.cj", "+ secondCount", "secondCount")),
        "libraryLabel" to listOf(
            location(f, "src/Consumer.cj", "import sample.binarypatterns.libraryLabel", "libraryLabel"),
            location(f, "src/Consumer.cj", "println(libraryLabel)", "libraryLabel")),
        "libraryFlag" to listOf(
            location(f, "src/Consumer.cj", "import sample.binarypatterns.libraryFlag", "libraryFlag"),
            location(f, "src/Consumer.cj", "if (libraryFlag)", "libraryFlag")),
    )

    private fun macroUses(f: LspWorkspaceFixture): Map<String, List<Location>> = linkedMapOf(
        "generatedCount" to listOf(
            location(f, "provider/src/Host.cj", "return generatedCount", "generatedCount"),
            location(f, "consumer/src/Consumer.cj", "generatedCount as firstCount", "generatedCount"),
            location(f, "consumer/src/Consumer.cj", "generatedCount as secondCount", "generatedCount"),
            location(f, "consumer/src/Consumer.cj", "return firstCount +", "firstCount"),
            location(f, "consumer/src/Consumer.cj", "+ secondCount", "secondCount")),
        "generatedLabel" to listOf(
            location(f, "provider/src/Host.cj", "return generatedLabel", "generatedLabel"),
            location(f, "consumer/src/Consumer.cj", "import sample.patternmacro.generatedLabel", "generatedLabel"),
            location(f, "consumer/src/Consumer.cj", "println(generatedLabel)", "generatedLabel")),
        "generatedFlag" to listOf(
            location(f, "provider/src/Host.cj", "return generatedFlag", "generatedFlag"),
            location(f, "consumer/src/Consumer.cj", "import sample.patternmacro.generatedFlag", "generatedFlag"),
            location(f, "consumer/src/Consumer.cj", "if (generatedFlag)", "generatedFlag")),
    )

    /** 原生协议的 PSI/API 短读；通知、协议 future 和 awaitIdle 全部留在读动作之外。 */
    private inline fun <R> readAnalyze(file: CjFile, crossinline action: CaSession.() -> R): R =
        ReadAction.compute<R, RuntimeException> { analyze(file) { action() } }

    private fun currentFile(fixture: LspWorkspaceFixture, path: String): CjFile = ReadAction.compute<CjFile, RuntimeException> {
        val structure = AnalysisApiLspProjectStructureState.getInstance(environment.project)
        structure.allSourceFiles.filterIsInstance<CjFile>().single { structure.documentUriOf(it) == fixture.uri(path) }
    }

    private fun assertNoErrors(session: LspIntegrationTestSession, fixture: LspWorkspaceFixture, path: String) {
        val errors = session.awaitPublishedDiagnostics(fixture.uri(path)).diagnostics.filter { it.severity == DiagnosticSeverity.Error }
        assertEquals(emptyList<Diagnostic>(), errors, "真实 fixture 的 error/severe 不能忽略")
    }

    /** 只按独立源码片段生成协议期望，绝不由产品 target key 计算期望。 */
    private fun location(f: LspWorkspaceFixture, path: String, anchor: String, token: String): Location {
        val text = f.text(path)
        val start = text.indexOf(anchor)
        check(start >= 0 && text.indexOf(anchor, start + 1) < 0)
        val tokenStart = anchor.indexOf(token)
        check(tokenStart >= 0)
        val doc = LspTextDocument(f.uri(path), "cangjie", 1, text)
        return Location(f.uri(path), Range(doc.positionAt(start + tokenStart), doc.positionAt(start + tokenStart + token.length)))
    }

    @OptIn(CompilerConfiguration.Internals::class)
    private fun nativeMacroOptions(sdk: Path, out: Path): LspMacroExpansionOptions {
        val processEnvironment = nativeEnvironment(sdk)
        val handle = SdkHandle(id = "test:${sdk.toAbsolutePath()}", homePath = sdk.toString(), environment = processEnvironment,
            macroServerPath = CangjieSdkFixture.requireMacroServer(sdk).toString())
        return LspMacroExpansionOptions(
            engine = FrontendMacroSemanticExpansionEngine(),
            constructionStartedListener = { nativeConstructions.add(NativeConstruction(it)) },
            publishListener = { session, _ -> recordNativeOutcome(session, MacroExpansionOutcome.PUBLISHED) },
            abandonedListener = { session, outcome -> recordNativeOutcome(session, outcome) },
            sdkProvider = { handle },
            fragmentParserInstaller = { module, configuration -> configuration.installDefaultMacroFragmentParserFactory(module.project) },
            externalMacroPackagesProvider = { setOf(FqName("patternmacros")) },
            compilerConfigurationProvider = { module, roots, _ ->
                CompilerConfiguration().apply {
                    languageVersionSettings = module.languageVersionSettings
                    targetPlatform = module.targetPlatform
                    moduleName = module.name
                    useLightTree = false
                    macroConstructionMode = MacroConstructionService.Mode.STRICT
                    macroSdkHome = sdk.toString()
                    macroCompilerEnvironment = processEnvironment
                    macroConstructionReadAccess = MacroConstructionReadAccess.Direct
                    macroExpansionDemandAutoCompilationEnabled = false
                    macroCompilationCacheContext = MacroCompilationCacheContext(
                        targetPlatform = module.targetPlatform,
                        runtimeLoaderEnvironmentFingerprint = "test:${sdk.toAbsolutePath()}",
                    )
                    roots.sourceRoots.forEach { addCangJieSourceRoot(it) }
                    roots.classpathRoots.forEach { addClasspathRoot(it) }
                    addClasspathRoot(out.toString())
                    macroArtifactPackages = listOf(MacroArtifactPackage(packageFqName = FqName("patternmacros"), kind = MacroArtifactPackage.Kind.MACRO,
                        cjoPath = out.resolve("patternmacros.cjo").toString(), dynamicLibPath = out.resolve("lib-macro_patternmacros.dll").toString(),
                        origin = MacroArtifactPackage.Origin.EXTERNAL_PATH))
                    macroExecutorFactory = MacroExecutorFactory {
                        LspMacroServerMacroExecutor(CangjieSdkFixture.requireMacroServer(sdk).toString(), sdk.toString(), processEnvironment)
                    }
                }
            },
        )
    }

    private fun nativeEnvironment(sdk: Path): Map<String, String> = CangjieSdkFixture.processEnvironment(sdk) + mapOf(
        "PATH" to listOf(sdk.resolve("bin"), sdk.resolve("runtime/lib/windows_x86_64_cjnative"), sdk.resolve("tools/bin"))
            .joinToString(";") + ";" + System.getenv("PATH"),
    )

    /** bounded native 进程，退出码/输出即 oracle；没有 executor 替身或手写二进制 metadata。 */
    private fun official(sdk: Path, directory: Path, vararg arguments: String) {
        val command = listOf(CangjieSdkFixture.requireCompiler(sdk).toString()) + arguments
        val process = ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true)
            .also { it.environment().putAll(nativeEnvironment(sdk)) }.start()
        val output = CompletableFuture.supplyAsync { process.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() } }
        if (!process.waitFor(45, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            assertTrue(process.waitFor(5, TimeUnit.SECONDS))
            fail<Unit>("官方命令超时：$command\n${output.get(5, TimeUnit.SECONDS)}")
        }
        assertEquals(0, process.exitValue(), "官方命令失败：$command\n${output.get(5, TimeUnit.SECONDS)}")
    }
}
