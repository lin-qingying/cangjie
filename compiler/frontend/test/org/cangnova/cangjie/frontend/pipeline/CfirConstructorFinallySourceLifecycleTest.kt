package org.cangnova.cangjie.frontend.pipeline

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassInitializationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirConstructorInitializationAvailability
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFieldInitializationState
import org.cangnova.cangjie.cfir.declarations.classInitializationInfo
import org.cangnova.cangjie.cfir.declarations.exportFacts
import org.cangnova.cangjie.cfir.declarations.parsingDiagnosticPublication
import org.cangnova.cangjie.cfir.declarations.parsingSourceContext
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticRelatedInformation
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithParameters1
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBreakExpression
import org.cangnova.cangjie.cfir.expressions.CfirContinueExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirThrowExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.configureLanguageVersionSettings
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

/**
 * B3 的 SOURCE 完成路径回归：真实 frontend 生产 ctor info，fixture 不构造或发布 flow/graph/state。
 *
 * 固定 K=3c7a5041f238102285764d370665f56f587f9dc4 的 CFG 用目标标签把 return/break/continue
 * 经 finally 后送到实际目标。Cangjie finally 必须执行；固定 N=14276740ffec1262b81b6b5236b1d840af270b37
 * TranslateFinallyRethrowFlows 则明确把 finally 的控制转移重新送到 rethrow。pending throw 因此保留，
 * 不能从 Kotlin 的异常覆盖规则替 Cangjie 推出正常构造出口。
 *
 * 原始无 initializer 源码已用 cjc 1.1.3 验证；其 ctorUninitMap 首次保存 return 的旧状态与
 * 跳转状态恢复会误报 finally 初始化。本测试的期待来自 docs、固定路线和独立执行对照，
 * 不把宿主 Sema 缺陷写成语言错误。执行对照的字段 initializer=0 与原回归输入严格分开。
 * 完整 cjc stdout/stderr、runtime 和 fixed-N 差异证据在 build/cjmp-invisible-peer-20261125/
 * constructor-finally-writer-cjc.json 与 constructor-finally-writer-authority.md。
 *
 * 两 parser 使用同一份未改写 body；检查实际 owner/ctor/receiver、正常出口数量与字段状态，
 * 并保留正确的缺字段负例。此文件未自行运行 Gradle，不代表 frontend 编译/实跑已通过。
 */
@OptIn(CompilerConfiguration.Internals::class, CfirImplementationDetail::class)
class CfirConstructorFinallySourceLifecycleTest {
    @TempDir
    lateinit var tempDir: Path

    @Test fun psiEarlyReturnRunsFinallyBeforeNormalExit() = assertSource("return_finally_initializes", false)
    @Test fun lightTreeEarlyReturnRunsFinallyBeforeNormalExit() = assertSource("return_finally_initializes", true)
    @Test fun psiFinallyThrowCancelsPendingReturn() = assertSource("finally_throw_cancels_return", false)
    @Test fun lightTreeFinallyThrowCancelsPendingReturn() = assertSource("finally_throw_cancels_return", true)
    @Test fun psiPendingThrowSurvivesFinallyReturn() = assertSource("finally_return_overrides_throw", false)
    @Test fun lightTreePendingThrowSurvivesFinallyReturn() = assertSource("finally_return_overrides_throw", true)
    @Test fun psiFinallyReturnReplacesPendingReturnOnce() = assertSource("finally_return_replaces_return", false)
    @Test fun lightTreeFinallyReturnReplacesPendingReturnOnce() = assertSource("finally_return_replaces_return", true)
    @Test fun psiPendingThrowWithFinallyReturnHasNoNormalFieldObligation() = assertSource("finally_return_without_initialization", false)
    @Test fun lightTreePendingThrowWithFinallyReturnHasNoNormalFieldObligation() = assertSource("finally_return_without_initialization", true)
    @Test fun psiNestedFinallyInitializesBothFieldsBeforeReturn() = assertSource("nested_finally_initializes", false)
    @Test fun lightTreeNestedFinallyInitializesBothFieldsBeforeReturn() = assertSource("nested_finally_initializes", true)
    @Test fun psiOuterFinallyThrowRemovesTheNormalExit() = assertSource("outer_finally_throw", false)
    @Test fun lightTreeOuterFinallyThrowRemovesTheNormalExit() = assertSource("outer_finally_throw", true)
    @Test fun psiSequentialFinallyKeepsCompletedFieldState() = assertSource("sequential_finally", false)
    @Test fun lightTreeSequentialFinallyKeepsCompletedFieldState() = assertSource("sequential_finally", true)
    @Test fun psiBreakCarriesFinallyFieldsToLoopExit() = assertSource("break_through_finally", false)
    @Test fun lightTreeBreakCarriesFinallyFieldsToLoopExit() = assertSource("break_through_finally", true)
    @Test fun psiContinueRunsFinallyWhoseBreakExitsTheLoop() = assertSource("continue_through_finally_break", false)
    @Test fun lightTreeContinueRunsFinallyWhoseBreakExitsTheLoop() = assertSource("continue_through_finally_break", true)
    @Test fun psiBreakFinallyKeepsTheRealMissingFieldError() = assertSource("break_finally_without_initialization", false)
    @Test fun lightTreeBreakFinallyKeepsTheRealMissingFieldError() = assertSource("break_finally_without_initialization", true)
    @Test fun psiOuterFinallyReturnPreservesInnerPendingThrow() = assertSource("nested_finally_return_overrides_throw", false)
    @Test fun lightTreeOuterFinallyReturnPreservesInnerPendingThrow() = assertSource("nested_finally_return_overrides_throw", true)
    @Test fun psiFinallyReturnWithoutAssignmentKeepsOneNormalExit() = assertSource("finally_return_replaces_return_without_initialization", false)
    @Test fun lightTreeFinallyReturnWithoutAssignmentKeepsOneNormalExit() = assertSource("finally_return_replaces_return_without_initialization", true)

    private fun assertSource(id: String, lightTree: Boolean) {
        val fixture = FIXTURES.single { it.id == id }
        assertEquals(fixture.nativeSha256, sha256(fixture.source), "核证源码及全部真实 body 必须保持原样")
        val directory = Files.createDirectories(tempDir.resolve(id + if (lightTree) "-light-tree" else "-psi"))
        val path = directory.resolve("source.cj")
        Files.writeString(path, fixture.source, StandardCharsets.UTF_8)
        val disposable = Disposer.newDisposable("constructor-finally-source-lifecycle")
        try {
            val messages = RecordingMessageCollector()
            val diagnostics = DiagnosticsCollectorImpl()
            val configuration = CompilerConfiguration().apply {
                messageCollector = messages
                diagnosticsCollector = diagnostics
                moduleName = "constructor_finally_source"
                useLightTree = lightTree
                assertTrue(configureLanguageVersionSettings("1.1.3"))
                addCangJieSourceRoot(path.toString())
            }
            val artifact = CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable))
            val records = diagnostics.diagnosticsByFilePath.flatMap { (file, values) ->
                values.map { DiagnosticRecord(file, it) }
            }
            val description = records.joinToString("\n") { it.diagnostic.factoryName + ": " + it.diagnostic.renderMessage() } +
                "\nmessages=" + messages.messages
            assertNotNull(artifact, description)
            assertFalse(messages.hasErrors(), description)
            val output = checkNotNull(artifact).frontendOutput.outputs.single()
            assertEquals(CfirSession.Kind.Source, output.session.kind)
            val file = output.fir.single()
            assertEquals(path.toAbsolutePath().normalize(), Path.of(checkNotNull(checkNotNull(file.sourceFile).path)).toAbsolutePath().normalize())
            assertSame(output.session, file.moduleData.session)
            assertTrue(file.origin.fromSource)
            val parser = checkNotNull(file.parsingSourceContext)
            assertEquals(fixture.source, parser.snapshot.text)
            parser.requireInput(fixture.source)
            assertTrue(checkNotNull(file.parsingDiagnosticPublication).diagnostics.none { it.isError })

            val owner = file.declarations.filterIsInstance<CfirClass>().single()
            assertEquals("Holder", owner.name.asString())
            assertSame(file, output.session.cfirProvider.getCfirClassifierContainerFileIfAny(owner.symbol))
            assertTrue(owner.origin.fromSource)
            val fields = owner.declarations.filterIsInstance<CfirFieldVariable>()
            assertEquals(fixture.fields, fields.map { it.name.asString() })
            fields.forEach {
                assertTrue(it.isVar)
                assertNull(it.initializer, "执行对照的 initializer=0 不能混入真实回归输入")
                assertTrue(it.returnTypeRef is CfirResolvedTypeRef)
            }
            val constructor = owner.declarations.filterIsInstance<CfirConstructor>().single()
            assertFalse(constructor.isPrimary)
            assertTrue(constructor.origin.fromSource)
            assertFalse(constructor.exportFacts.isCompilerAdded)
            assertFalse(constructor.exportFacts.isImplicitlyAdded)
            assertSame(CjRealSourceElementKind, checkNotNull(constructor.source).kind)
            assertEquals(fixture.parameters, constructor.valueParameters.map { it.name.asString() })
            assertRealBody(fixture, constructor)

            val info = checkNotNull(owner.classInitializationInfo(output.session)) {
                "正式 Source checker 必须发布实际 owner 完成产品：$description"
            }
            assertSame(owner, info.owner)
            assertSame(output.session, info.useSiteSession)
            assertEquals(CfirClassInitializationOrigin.SOURCE, info.origin)
            assertTrue(info.isCurrent(), "刚完成的 Source 产品必须仍对应当前输入")
            assertFalse(info.hasUnavailableConstructors)
            assertEquals(setOf(constructor.symbol), info.constructors.keys)
            val completed = info.constructors.getValue(constructor.symbol)
            assertSame(constructor, completed.constructor)
            assertSame(owner.symbol, completed.receiverOwner)
            assertEquals(CfirConstructorInitializationAvailability.COMPLETE, completed.availability)
            val actualExits = completed.normalExitFields.map { exit ->
                fields.associate { field -> field.name.asString() to exit.getValue(field.symbol) }
            }
            val dataDescription = description + "\nnormalExitFields=" + actualExits +
                "\nterminalFields=" + fields.associate { it.name.asString() to completed.terminalFields.getValue(it.symbol) }
            assertEquals(fixture.normalExitCount, completed.normalExitFields.size, dataDescription)
            assertEquals(fixture.normalExitCount > 0, completed.hasNormalExit, dataDescription)
            assertEquals(fixture.normalExitCount > 0, info.hasNormalConstructionExit, dataDescription)
            for (field in fields) {
                assertEquals(CfirFieldInitializationState(false, false), info.initialFields.getValue(field.symbol),
                    "本回归所有实例字段在 ctor 前都没有 initializer")
                for (exit in completed.normalExitFields) {
                    val expected = fixture.normalFields.getValue(field.name.asString())
                    assertEquals(CfirFieldInitializationState(expected, expected), exit.getValue(field.symbol), dataDescription)
                }
                fixture.terminalFields[field.name.asString()]?.let { expected ->
                    assertEquals(CfirFieldInitializationState(expected, expected),
                        completed.terminalFields.getValue(field.symbol), dataDescription)
                }
                if (fixture.normalExitCount > 0) {
                    val expected = fixture.normalFields.getValue(field.name.asString())
                    assertEquals(CfirFieldInitializationState(expected, expected), info.fieldState(field.symbol), dataDescription)
                }
            }
            // 未构造任何初始化 info；诊断必须与同一份实际正常出口数据一致。
            assertDiagnostics(fixture, path, records, dataDescription)
            val success = !CheckCompilationErrors.CheckDiagnosticCollector.checkHasErrors(configuration)
            assertEquals(fixture.missingFields.isEmpty(), success, dataDescription)
        } finally {
            Disposer.dispose(disposable)
        }
    }

    /** 保留原 Source 的 finally block 及 jump target，不能仅靠 parser 文本使测试通过。 */
    private fun assertRealBody(fixture: Fixture, constructor: CfirConstructor) {
        val declaration = sourceText(fixture.source, checkNotNull(constructor.source))
        val body = checkNotNull(constructor.body)
        assertEquals(declaration.substring(declaration.indexOf('{')).trim(), sourceText(fixture.source, checkNotNull(body.source)))
        val observed = mutableListOf<Statement>()
        var finallyBlocks = 0
        body.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this)
            }
            private fun record(kind: String, source: CjSourceElement?) {
                if (source != null && source.kind === CjRealSourceElementKind) {
                    observed += Statement(kind, sourceText(fixture.source, source))
                }
            }
            override fun visitTryExpression(expression: CfirTryExpression) {
                val finallyBlock = checkNotNull(expression.finallyBlock)
                assertNotNull(finallyBlock.source)
                finallyBlocks++
                super.visitTryExpression(expression)
            }
            override fun visitAssignment(assignment: CfirAssignment) {
                record("assignment", assignment.source)
                super.visitAssignment(assignment)
            }
            override fun visitReturnExpression(expression: CfirReturnExpression) {
                record("return", expression.source)
                assertTrue(expression.target.isBound)
                assertSame(constructor, expression.target.labeledElement, "return 的最终目标仍是实际 ctor")
                super.visitReturnExpression(expression)
            }
            override fun visitThrowExpression(expression: CfirThrowExpression) {
                record("throw", expression.source)
                super.visitThrowExpression(expression)
            }
            override fun visitBreakExpression(expression: CfirBreakExpression) {
                record("break", expression.source)
                assertTrue(expression.target.isBound)
                val actualLoop = assertInstanceOf(CfirLoopExpression::class.java, expression.target.labeledElement)
                assertFalse(actualLoop.isDoWhile)
                super.visitBreakExpression(expression)
            }
            override fun visitContinueExpression(expression: CfirContinueExpression) {
                record("continue", expression.source)
                assertTrue(expression.target.isBound)
                val actualLoop = assertInstanceOf(CfirLoopExpression::class.java, expression.target.labeledElement)
                assertFalse(actualLoop.isDoWhile)
                super.visitContinueExpression(expression)
            }
        })
        assertEquals(fixture.finallyBlocks, finallyBlocks)
        assertEquals(fixture.statements.sortedBy(Statement::sortKey), observed.sortedBy(Statement::sortKey))
    }

    private fun assertDiagnostics(fixture: Fixture, path: Path, records: List<DiagnosticRecord>, description: String) {
        val init = fixture.source.indexOf("public init(") + "public ".length
        val expected = fixture.missingFields.map {
            DiagnosticKey("CFIR_CLASS_UNINITIALIZED_FIELD", path.toAbsolutePath().normalize(), TextRange(init, init + 4), it)
        }
        val actual = records.map { record ->
            val diagnostic = record.diagnostic
            assertTrue(diagnostic.severity.isError, description)
            assertTrue(diagnostic.isValid, description)
            assertEquals(emptyList<CjDiagnosticRelatedInformation>(), diagnostic.relatedInformation)
            val field = assertInstanceOf(CjDiagnosticWithParameters1::class.java, diagnostic).a
            assertInstanceOf(Name::class.java, field)
            DiagnosticKey(diagnostic.factoryName, Path.of(checkNotNull(record.filePath)).toAbsolutePath().normalize(),
                diagnostic.firstRange, (field as Name).asString())
        }
        assertEquals(expected.sortedBy(DiagnosticKey::sortKey), actual.sortedBy(DiagnosticKey::sortKey), description)
    }

    private fun sourceText(text: String, source: CjSourceElement): String {
        require(source.startOffset in 0..text.length && source.endOffset in source.startOffset..text.length)
        return text.substring(source.startOffset, source.endOffset).trim()
    }

    private data class Statement(val kind: String, val text: String) {
        fun sortKey(): String = kind + ":" + text
    }
    private data class Fixture(
        val id: String,
        val source: String,
        val nativeSha256: String,
        val fields: List<String>,
        val parameters: List<String>,
        val normalExitCount: Int,
        val normalFields: Map<String, Boolean>,
        val terminalFields: Map<String, Boolean> = emptyMap(),
        val missingFields: List<String>,
        val statements: List<Statement>,
        val finallyBlocks: Int,
    )
    private data class DiagnosticRecord(val filePath: String?, val diagnostic: CjDiagnostic)
    private data class DiagnosticKey(val factory: String, val path: Path, val range: TextRange, val field: String) {
        fun sortKey(): String = factory + ":" + path + ":" + range + ":" + field
    }
    private class RecordingMessageCollector : MessageCollector {
        val messages = mutableListOf<Pair<CompilerMessageSeverity, String>>()
        override fun clear() = messages.clear()
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
            messages += severity to message
        }
        override fun hasErrors(): Boolean = messages.any { it.first.isError }
    }

    companion object {
        private fun sha256(text: String): String = MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }

        private val FIXTURES: List<Fixture> = listOf(
            Fixture(
                id = "return_finally_initializes",
                source = """
                    package constructor_finally_source
                    
                    public class Holder {
                        public var value: Int64
                    
                        public init() {
                            try {
                                return
                            } finally {
                                this.value = 1
                            }
                        }
                    }
                """.trimIndent() + "\n",
                nativeSha256 = "9b308be96b1bf1ad0c2ceaea97da3d26d458c7dc5a026cc5ebf598ca3308a049",
                fields = listOf("value"),
                parameters = listOf(),
                normalExitCount = 1,
                normalFields = mapOf("value" to true),
                missingFields = listOf(),
                statements = listOf(Statement("return", "return"), Statement("assignment", "this.value = 1")),
                finallyBlocks = 1,
            ),
            Fixture(
                id = "finally_throw_cancels_return",
                source = """
                    package constructor_finally_source
                    
                    public class Holder {
                        public var value: Int64
                    
                        public init(error: Exception) {
                            try {
                                return
                            } finally {
                                throw error
                            }
                        }
                    }
                """.trimIndent() + "\n",
                nativeSha256 = "fa03866bfa0894a5f73cd2a02912852a9d3aedfd29378dd4334d9856dc10be5c",
                fields = listOf("value"),
                parameters = listOf("error"),
                normalExitCount = 0,
                normalFields = mapOf(),
                missingFields = listOf(),
                statements = listOf(Statement("return", "return"), Statement("throw", "throw error")),
                finallyBlocks = 1,
            ),
            Fixture(
                id = "finally_return_overrides_throw",
                source = """
                    package constructor_finally_source
                    
                    public class Holder {
                        public var value: Int64
                    
                        public init(error: Exception) {
                            try {
                                throw error
                            } finally {
                                this.value = 1
                                return
                            }
                        }
                    }
                """.trimIndent() + "\n",
                nativeSha256 = "1b848a0c6c2f019ef84f84346380c10c4dcfb5d97c9a7c96ef6325d522ac0c2f",
                fields = listOf("value"),
                parameters = listOf("error"),
                normalExitCount = 0,
                normalFields = mapOf(),
                terminalFields = mapOf("value" to true),
                missingFields = listOf(),
                statements = listOf(Statement("throw", "throw error"), Statement("assignment", "this.value = 1"), Statement("return", "return")),
                finallyBlocks = 1,
            ),
            Fixture(
                id = "finally_return_replaces_return",
                source = """
                    package constructor_finally_source
                    
                    public class Holder {
                        public var value: Int64
                    
                        public init() {
                            try {
                                return
                            } finally {
                                this.value = 1
                                return
                            }
                        }
                    }
                """.trimIndent() + "\n",
                nativeSha256 = "4ab4c09d05d19a643b1e9f72eb1d2a0826f4c3c64e35ae0ca0a80760f39a302d",
                fields = listOf("value"),
                parameters = listOf(),
                normalExitCount = 1,
                normalFields = mapOf("value" to true),
                missingFields = listOf(),
                statements = listOf(Statement("return", "return"), Statement("assignment", "this.value = 1"), Statement("return", "return")),
                finallyBlocks = 1,
            ),
            Fixture(
                id = "finally_return_without_initialization",
                source = """
                    package constructor_finally_source
                    
                    public class Holder {
                        public var value: Int64
                    
                        public init(error: Exception) {
                            try {
                                throw error
                            } finally {
                                return
                            }
                        }
                    }
                """.trimIndent() + "\n",
                nativeSha256 = "a46f7b7ebf50d05c8d16d541e3490eadd12012ddcaf46eecf557176b82d5777c",
                fields = listOf("value"),
                parameters = listOf("error"),
                normalExitCount = 0,
                normalFields = mapOf(),
                terminalFields = mapOf("value" to false),
                missingFields = listOf(),
                statements = listOf(Statement("throw", "throw error"), Statement("return", "return")),
                finallyBlocks = 1,
            ),
            Fixture(
                id = "nested_finally_initializes",
                source = """
                    package constructor_finally_source
                    
                    public class Holder {
                        public var value: Int64
                        public var copy: Int64
                    
                        public init() {
                            try {
                                try {
                                    return
                                } finally {
                                    this.value = 1
                                }
                            } finally {
                                this.copy = this.value
                            }
                        }
                    }
                """.trimIndent() + "\n",
                nativeSha256 = "7e1052eca3e08b07ef814bc6312573836894cd5511aa6dcc236478e54eca1640",
                fields = listOf("value", "copy"),
                parameters = listOf(),
                normalExitCount = 1,
                normalFields = mapOf("value" to true, "copy" to true),
                missingFields = listOf(),
                statements = listOf(Statement("return", "return"), Statement("assignment", "this.value = 1"), Statement("assignment", "this.copy = this.value")),
                finallyBlocks = 2,
            ),
            Fixture(
                id = "outer_finally_throw",
                source = """
                    package constructor_finally_source
                    
                    public class Holder {
                        public var value: Int64
                    
                        public init(error: Exception) {
                            try {
                                try {
                                    return
                                } finally {
                                    this.value = 1
                                }
                            } finally {
                                throw error
                            }
                        }
                    }
                """.trimIndent() + "\n",
                nativeSha256 = "12b25501175bfd5df1b27997ac04d0a4d36dae6f2d2747807d52309e95e8c8ba",
                fields = listOf("value"),
                parameters = listOf("error"),
                normalExitCount = 0,
                normalFields = mapOf(),
                terminalFields = mapOf("value" to true),
                missingFields = listOf(),
                statements = listOf(Statement("return", "return"), Statement("assignment", "this.value = 1"), Statement("throw", "throw error")),
                finallyBlocks = 2,
            ),
            Fixture(
                id = "sequential_finally",
                source = """
                    package constructor_finally_source
                    
                    public class Holder {
                        public var value: Int64
                        public var copy: Int64
                    
                        public init() {
                            try {
                                this.value = 1
                            } finally {
                                this.value = 2
                            }
                            this.copy = this.value
                        }
                    }
                """.trimIndent() + "\n",
                nativeSha256 = "64e4345fbb0237d9347e584545841205252fbaa060b00ff12c805efde992d99a",
                fields = listOf("value", "copy"),
                parameters = listOf(),
                normalExitCount = 1,
                normalFields = mapOf("value" to true, "copy" to true),
                missingFields = listOf(),
                statements = listOf(Statement("assignment", "this.value = 1"), Statement("assignment", "this.value = 2"), Statement("assignment", "this.copy = this.value")),
                finallyBlocks = 1,
            ),
            Fixture(
                id = "break_through_finally",
                source = """
                    package constructor_finally_source
                    
                    public class Holder {
                        public var value: Int64
                        public var copy: Int64
                    
                        public init() {
                            while (true) {
                                try {
                                    break
                                } finally {
                                    this.value = 1
                                }
                            }
                            this.copy = this.value
                        }
                    }
                """.trimIndent() + "\n",
                nativeSha256 = "c4896119e72e6618cc822129453bf12cf12f78c1cae5bab35e966bada75a84d3",
                fields = listOf("value", "copy"),
                parameters = listOf(),
                normalExitCount = 1,
                normalFields = mapOf("value" to true, "copy" to true),
                missingFields = listOf(),
                statements = listOf(Statement("break", "break"), Statement("assignment", "this.value = 1"), Statement("assignment", "this.copy = this.value")),
                finallyBlocks = 1,
            ),
            Fixture(
                id = "continue_through_finally_break",
                source = """
                    package constructor_finally_source
                    
                    public class Holder {
                        public var value: Int64
                        public var copy: Int64
                    
                        public init() {
                            while (true) {
                                try {
                                    continue
                                } finally {
                                    this.value = 1
                                    break
                                }
                            }
                            this.copy = this.value
                        }
                    }
                """.trimIndent() + "\n",
                nativeSha256 = "f261c721ba0c0ec839ad7b4562fc1c2da9e1b229b6477f8cc08a82ee8fd961f0",
                fields = listOf("value", "copy"),
                parameters = listOf(),
                normalExitCount = 1,
                normalFields = mapOf("value" to true, "copy" to true),
                missingFields = listOf(),
                statements = listOf(Statement("continue", "continue"), Statement("assignment", "this.value = 1"), Statement("break", "break"), Statement("assignment", "this.copy = this.value")),
                finallyBlocks = 1,
            ),
            Fixture(
                id = "break_finally_without_initialization",
                source = """
                    package constructor_finally_source
                    
                    public class Holder {
                        public var value: Int64
                    
                        public init() {
                            while (true) {
                                try {
                                    break
                                } finally {
                                    let _ = 1
                                }
                            }
                        }
                    }
                """.trimIndent() + "\n",
                nativeSha256 = "a94f240e4680a429b5fd45160ff01d66557f89bd9d5d607fd53cab9e97ae8ccb",
                fields = listOf("value"),
                parameters = listOf(),
                normalExitCount = 1,
                normalFields = mapOf("value" to false),
                missingFields = listOf("value"),
                statements = listOf(Statement("break", "break")),
                finallyBlocks = 1,
            ),
            Fixture(
                id = "nested_finally_return_overrides_throw",
                source = """
                    package constructor_finally_source
                    
                    public class Holder {
                        public var value: Int64
                        public var copy: Int64
                    
                        public init(error: Exception) {
                            try {
                                try {
                                    return
                                } finally {
                                    this.value = 1
                                    throw error
                                }
                            } finally {
                                this.copy = this.value
                                return
                            }
                        }
                    }
                """.trimIndent() + "\n",
                nativeSha256 = "8cef34b29b414be4bd0d12c6639a5a44db273472a3502352a606865fe4a962b6",
                fields = listOf("value", "copy"),
                parameters = listOf("error"),
                normalExitCount = 0,
                normalFields = mapOf(),
                terminalFields = mapOf("value" to true, "copy" to true),
                missingFields = listOf(),
                statements = listOf(Statement("return", "return"), Statement("assignment", "this.value = 1"), Statement("throw", "throw error"), Statement("assignment", "this.copy = this.value"), Statement("return", "return")),
                finallyBlocks = 2,
            ),
            Fixture(
                id = "finally_return_replaces_return_without_initialization",
                source = """
                    package constructor_finally_source
                    
                    public class Holder {
                        public var value: Int64
                    
                        public init() {
                            try {
                                return
                            } finally {
                                return
                            }
                        }
                    }
                """.trimIndent() + "\n",
                nativeSha256 = "9f9685a53a416fa6faad45afdd5d654f964ff9be973db98f852008f090af5b34",
                fields = listOf("value"),
                parameters = listOf(),
                normalExitCount = 1,
                normalFields = mapOf("value" to false),
                missingFields = listOf("value"),
                statements = listOf(Statement("return", "return"), Statement("return", "return")),
                finallyBlocks = 1,
            ),
        )
    }
}
