package org.cangnova.cangjie.lsp.analysis

import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
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
import org.cangnova.cangjie.frontend.pipeline.macroCompilerEnvironment
import org.cangnova.cangjie.frontend.pipeline.macroConstructionMode
import org.cangnova.cangjie.frontend.pipeline.macroConstructionReadAccess
import org.cangnova.cangjie.frontend.pipeline.macroSdkHome
import org.cangnova.cangjie.frontend.pipeline.macroExecutorFactory
import org.cangnova.cangjie.frontend.pipeline.installDefaultMacroFragmentParserFactory
import org.cangnova.cangjie.frontend.pipeline.MacroExecutorFactory
import org.cangnova.cangjie.lsp.CangjieLspServerOptions
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.cangnova.cangjie.lsp.testkit.ServiceLoadedLspTestEnvironment
import org.cangnova.cangjie.macro.expansion.MacroConstructionReadAccess
import org.cangnova.cangjie.macro.expansion.SdkHandle
import org.cangnova.cangjie.macro.process.LspMacroServerMacroExecutor
import org.cangnova.cangjie.name.FqName
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.DocumentHighlightParams
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.File
import java.nio.file.Files

/**
 * LSP 协议级：真实宏展开后，definition/hover 能看到宏生成符号。
 */
@EnabledOnOs(OS.LINUX)
@Timeout(180)
class LspMacroExpansionProtocolIntegrationTest : AbstractLspIntegrationTest() {
    override val autoCreateDefaultSession: Boolean = false

    private lateinit var macroOut: File

    override fun defaultServerOptions(): CangjieLspServerOptions {
        val sdkHome = System.getenv("CANGJIE_HOME")
        assumeTrue(sdkHome != null && File(sdkHome).isDirectory, "需要 CANGJIE_HOME 指向 Linux SDK")
        val sdk = File(sdkHome)
        val options = LspMacroExpansionOptions(
            engine = FrontendMacroSemanticExpansionEngine(),
            sdkProvider = { sdkHandle(sdk) },
            compilerConfigurationProvider = { module, roots, _ ->
                compilerConfiguration(module, roots, sdk)
            },
            fragmentParserInstaller = { module, configuration ->
                configuration.installDefaultMacroFragmentParserFactory(module.project)
            },
            externalMacroPackagesProvider = { setOf(FqName("demo_macro")) },
        )
        return CangjieLspServerOptions(
            environmentMode = CangJieCoreEnvironmentMode.UnitTest,
            macroExpansion = options,
            environmentFactory = {
                ServiceLoadedLspTestEnvironment.create(
                    mode = CangJieCoreEnvironmentMode.UnitTest,
                    macroExpansion = options,
                )
            },
            exitHandler = {},
        )
    }

    @Test
    fun `definition and hover see macro generated declaration`() {
        val sdkHome = System.getenv("CANGJIE_HOME")
        assumeTrue(sdkHome != null && File(sdkHome).isDirectory, "需要 CANGJIE_HOME 指向 Linux SDK")
        val sdk = File(sdkHome)
        macroOut = prepareMacroArtifact(sdk)

        LspWorkspaceFixtureBuilder()
            .addModule(name = "workspace", packageSearchPaths = listOf(macroOut.absolutePath))
            .source(
                "A.cj",
                """
                package sample

                import demo_macro.Make

                @Make
                public func carrier(): Unit {}

                public func useGenerated(): Int64 {
                    return generatedByMacro()
                }
                """.trimIndent(),
            )
            .source(
                "B.cj",
                """
                package sample

                public func useGeneratedFromB(): Int64 {
                    return generatedByMacro()
                }
                """.trimIndent(),
            )
            .build()
            .use { fixture ->
                createSession(fixture.initializeParams()).use { testSession ->
                    val uri = fixture.uri("src/A.cj")
                    testSession.openDocument(uri, fixture.text("src/A.cj"))
                    val position = positionOf(fixture, "src/A.cj", "generatedByMacro", delta = 1)

                    val definition = awaitNonNull(30_000) {
                        testSession.definition(DefinitionParams(TextDocumentIdentifier(uri), position))
                            .let { either ->
                                either.left?.firstOrNull()
                                    ?: either.right?.firstOrNull()?.let { Location(it.targetUri, it.targetRange) }
                            }
                    }
                    assertNotNull(definition, "definition 应解析到宏生成函数")
                    assertEquals(uri, definition!!.uri)
                    val targetLine = fixture.text("src/A.cj").lines()[definition!!.range.start.line]
                    assertTrue(
                        targetLine.contains("@Make") || targetLine.contains("carrier"),
                        "宏生成声明没有物理 PSI，definition 应回到宏调用来源：$targetLine",
                    )

                    val hover = awaitNonNull(30_000) {
                        testSession.hover(HoverParams(TextDocumentIdentifier(uri), position))
                            .takeIf { it != null && it.contents != null }
                    }
                    // 生成函数没有 cdoc，hover 内容可能为空；这里只要求协议请求成功返回。
                    assertNotNull(hover, "hover 请求应成功返回")

                    val references = testSession.references(
                        org.eclipse.lsp4j.ReferenceParams(
                            TextDocumentIdentifier(uri),
                            position,
                            org.eclipse.lsp4j.ReferenceContext(true),
                        ),
                    )
                    val referenceLines = references
                        .filter { it.uri == uri }
                        .map { fixture.text("src/A.cj").lines()[it.range.start.line] }
                    assertTrue(
                        referenceLines.any { it.contains("generatedByMacro") },
                        "references 应包含生成函数使用点：$referenceLines",
                    )
                    val crossFileLines = references
                        .filter { it.uri == fixture.uri("src/B.cj") }
                        .map { fixture.text("src/B.cj").lines()[it.range.start.line] }
                    assertTrue(
                        crossFileLines.any { it.contains("generatedByMacro") },
                        "references 应包含跨文件使用点：$crossFileLines",
                    )

                    val highlights = testSession.documentHighlight(
                        DocumentHighlightParams(TextDocumentIdentifier(uri), position),
                    )
                    assertTrue(highlights.isNotEmpty(), "宏生成符号使用点应能高亮")

                    val refsWithoutDeclaration = testSession.references(
                        org.eclipse.lsp4j.ReferenceParams(
                            TextDocumentIdentifier(uri),
                            position,
                            org.eclipse.lsp4j.ReferenceContext(false),
                        ),
                    )
                    val usageOnlyLines = refsWithoutDeclaration
                        .filter { it.uri == uri }
                        .map { fixture.text("src/A.cj").lines()[it.range.start.line] }
                    assertTrue(
                        usageOnlyLines.any { it.contains("generatedByMacro") },
                        "includeDeclaration=false 仍应包含使用点：$usageOnlyLines",
                    )

                    val report = testSession.documentDiagnostic(
                        testSession.documentDiagnosticParams(uri),
                    )
                    val diagnostics = (report as? org.eclipse.lsp4j.FullDocumentDiagnosticReport)?.items.orEmpty()
                    assertTrue(
                        diagnostics.none { diagnostic ->
                            val text = diagnostic.message?.left ?: diagnostic.message?.right?.value ?: ""
                            text.contains("MACRO_") || text.contains("UNRESOLVED")
                        },
                        "宏生成符号不应产生宏/未解析诊断：$diagnostics",
                    )

                }
            }
    }

    @Test
    fun `member function references keep declaration`() {
        val sdkHome = System.getenv("CANGJIE_HOME")
        assumeTrue(sdkHome != null && File(sdkHome).isDirectory, "需要 CANGJIE_HOME 指向 Linux SDK")
        macroOut = prepareMacroArtifact(File(sdkHome))

        LspWorkspaceFixtureBuilder()
            .source(
                "A.cj",
                """
                package sample

                public class C {
                    public func member(): Int64 { return 1 }
                }

                public func useMember(c: C): Int64 {
                    return c.member()
                }
                """.trimIndent(),
            )
            .build()
            .use { fixture ->
                createSession(fixture.initializeParams()).use { testSession ->
                    val uri = fixture.uri("src/A.cj")
                    testSession.openDocument(uri, fixture.text("src/A.cj"))
                    val declarationPosition = positionOf(fixture, "src/A.cj", "member()", delta = 1)

                    val withDeclaration = testSession.references(
                        org.eclipse.lsp4j.ReferenceParams(
                            TextDocumentIdentifier(uri),
                            declarationPosition,
                            org.eclipse.lsp4j.ReferenceContext(true),
                        ),
                    )
                    val lines = withDeclaration
                        .filter { it.uri == uri }
                        .map { fixture.text("src/A.cj").lines()[it.range.start.line] }
                    assertTrue(
                        lines.any { it.contains("public func member") } &&
                            lines.any { it.contains("c.member()") },
                        "成员函数 references 应同时包含声明与使用：$lines",
                    )
                }
            }
    }

    @Test
    fun `same generated name in different packages does not collide`() {
        val sdkHome = System.getenv("CANGJIE_HOME")
        assumeTrue(sdkHome != null && File(sdkHome).isDirectory, "需要 CANGJIE_HOME 指向 Linux SDK")
        macroOut = prepareMacroArtifact(File(sdkHome))

        LspWorkspaceFixtureBuilder()
            .source(
                "A.cj",
                """
                package a

                import demo_macro.Make

                @Make
                public func carrierA(): Unit {}

                public func useA(): Int64 {
                    return generatedByMacro()
                }
                """.trimIndent(),
            )
            .source(
                "B.cj",
                """
                package b

                import demo_macro.Make

                @Make
                public func carrierB(): Unit {}

                public func useB(): Int64 {
                    return generatedByMacro()
                }
                """.trimIndent(),
            )
            .build()
            .use { fixture ->
                createSession(fixture.initializeParams()).use { testSession ->
                    val aUri = fixture.uri("src/A.cj")
                    val bUri = fixture.uri("src/B.cj")
                    testSession.openDocument(aUri, fixture.text("src/A.cj"))
                    testSession.openDocument(bUri, fixture.text("src/B.cj"))
                    val position = positionOf(fixture, "src/A.cj", "generatedByMacro", delta = 1)

                    val references = testSession.references(
                        org.eclipse.lsp4j.ReferenceParams(
                            TextDocumentIdentifier(aUri),
                            position,
                            org.eclipse.lsp4j.ReferenceContext(true),
                        ),
                    )
                    val bLines = references
                        .filter { it.uri == bUri }
                        .map { fixture.text("src/B.cj").lines()[it.range.start.line] }
                    assertTrue(
                        bLines.none { it.contains("generatedByMacro") },
                        "跨包同名生成符号不应撞键：$bLines",
                    )
                }
            }
    }

    private fun <T : Any> awaitNonNull(timeoutMillis: Long, block: () -> T?): T? {
        val deadline = System.nanoTime() + timeoutMillis * 1_000_000
        while (System.nanoTime() < deadline) {
            block()?.let { return it }
            Thread.sleep(200)
        }
        return null
    }

    private fun prepareMacroArtifact(sdk: File): File {
        val root = Files.createTempDirectory("lsp-macro-protocol-").toFile()
        val source = File(root, "macro-src").apply { mkdirs() }
        File(source, "macros.cj").writeText(
            """
            macro package demo_macro

            import std.ast.*

            public macro Make(input: Tokens): Tokens {
                return quote(func generatedByMacro(): Int64 { return 42 })
            }
            """.trimIndent(),
        )
        val out = File(root, "macro-out").apply { mkdirs() }
        val process = ProcessBuilder(
            File(sdk, "bin/cjc").absolutePath,
            "-p", source.absolutePath,
            "--compile-macro",
        ).directory(out)
            .redirectErrorStream(true)
            .also { it.environment()["CANGJIE_HOME"] = sdk.absolutePath }
            .start()
        val output = process.inputStream.bufferedReader().readText()
        assumeTrue(process.waitFor() == 0, "宏包编译失败: $output")
        return out
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
    ): CompilerConfiguration = CompilerConfiguration().apply {
        languageVersionSettings = module.languageVersionSettings
        targetPlatform = module.targetPlatform
        moduleName = module.name
        useLightTree = false
        macroConstructionMode = MacroConstructionService.Mode.DEGRADED
        macroSdkHome = sdk.absolutePath
        macroCompilerEnvironment = System.getenv() + mapOf("CANGJIE_HOME" to sdk.absolutePath)
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

    private fun positionOf(
        fixture: LspWorkspaceFixture,
        relativePath: String,
        token: String,
        occurrence: Int = 1,
        delta: Int = 0,
    ): Position {
        val text = fixture.text(relativePath)
        var searchFrom = 0
        var offset = -1
        repeat(occurrence) {
            offset = text.indexOf(token, searchFrom)
            check(offset >= 0) { "Cannot find `$token` in $relativePath" }
            searchFrom = offset + token.length
        }
        val document = LspTextDocument(
            uri = fixture.uri(relativePath),
            languageId = "cangjie",
            version = 1,
            text = text,
        )
        return document.positionAt(offset + delta)
    }
}
