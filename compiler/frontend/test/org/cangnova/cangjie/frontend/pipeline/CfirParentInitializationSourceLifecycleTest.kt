package org.cangnova.cangjie.frontend.pipeline

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.exportFacts
import org.cangnova.cangjie.cfir.declarations.parsingDiagnosticPublication
import org.cangnova.cangjie.cfir.declarations.parsingSourceContext
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticRelatedInformation
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithParameters1
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithSource
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirThrowExpression
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
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
 * 实际 SOURCE 父 owner 初始化的普通 frontend/checker 回归；两个 parser 消费同一份真实源码。
 *
 * N=14276740ffec1262b81b6b5236b1d840af270b37，K=3c7a5041f238102285764d370665f56f587f9dc4。
 * 依据 r20-parent-initialization-publication-authority.md 的父先完成、逐 ctor 检查及正常出口规则，
 * 并由 1.1.3 cjc 的完整 stdout/stderr 核证输入；证据在 build/cjmp-invisible-peer-20261125/
 * r20-parent-initialization-source-writer-cjc.json。这里不构造初始化数据、symbol provider 或诊断。
 *
 * CLASS_UNINITIALIZED_FIELD 的 N 诊断 owner 是真实 ctor；CFIR 公开的 ACTUAL_DECLARATION_NAME
 * 合同将其定位到 init token。cjc 旧诊断的声明首字符范围另行记录，不冒充 CFIR 名称范围。
 * 隐式 default 的 NO_NON_PARAM 范围按固定 N TypeChecker 的显式 range 保留在子类名。
 *
 * 多 ctor 原始变体的末次字段状态会污染子诊断，已单独留报告；本文件保留独立 ctor 的真实错误，
 * 并用子类实际写继承字段的病例检查父诊断不能被覆盖。全部期待比较完整多重集，不过滤额外错误。
 * 编译结果由正式 frontend 阶段产生；测试不调用构造器，不代表 native CLI 执行或 Gradle 验收。
 */
@OptIn(CompilerConfiguration.Internals::class, CfirImplementationDetail::class)
class CfirParentInitializationSourceLifecycleTest {
    @TempDir
    lateinit var tempDir: Path

    @Test fun psiParentDeclaredAfterChildPublishesBeforeSuperRead() = assertSource("declared_after", false)
    @Test fun lightTreeParentDeclaredAfterChildPublishesBeforeSuperRead() = assertSource("declared_after", true)
    @Test fun psiParentInLaterSourceFilePublishesBeforeSuperRead() = assertSource("cross_file", false)
    @Test fun lightTreeParentInLaterSourceFilePublishesBeforeSuperRead() = assertSource("cross_file", true)
    @Test fun psiReversedSourceRootsKeepTheActualParent() = assertSource("cross_file_reverse", false)
    @Test fun lightTreeReversedSourceRootsKeepTheActualParent() = assertSource("cross_file_reverse", true)
    @Test fun psiImplicitDefaultReportsOnlyMissingNonParameterSuper() = assertSource("implicit_default", false)
    @Test fun lightTreeImplicitDefaultReportsOnlyMissingNonParameterSuper() = assertSource("implicit_default", true)
    @Test fun psiFirstIncompleteParentConstructorKeepsItsNativeFieldError() = assertSource("multiple_isolated_bad_first", false)
    @Test fun lightTreeFirstIncompleteParentConstructorKeepsItsNativeFieldError() = assertSource("multiple_isolated_bad_first", true)
    @Test fun psiLastIncompleteParentConstructorKeepsItsNativeFieldError() = assertSource("multiple_isolated_bad_last", false)
    @Test fun lightTreeLastIncompleteParentConstructorKeepsItsNativeFieldError() = assertSource("multiple_isolated_bad_last", true)
    @Test fun psiChildWritesCannotEraseTheLaterParentConstructorError() = assertSource("child_writes_parent_field", false)
    @Test fun lightTreeChildWritesCannotEraseTheLaterParentConstructorError() = assertSource("child_writes_parent_field", true)
    @Test fun psiEarlyNormalReturnKeepsTheParentFieldError() = assertSource("early_return", false)
    @Test fun lightTreeEarlyNormalReturnKeepsTheParentFieldError() = assertSource("early_return", true)
    @Test fun psiAllNormalReturnsInitializeTheParentField() = assertSource("early_return_initialized", false)
    @Test fun lightTreeAllNormalReturnsInitializeTheParentField() = assertSource("early_return_initialized", true)
    @Test fun psiThrowOnlyConstructorHasNoNormalInitializationExit() = assertSource("throw_only", false)
    @Test fun lightTreeThrowOnlyConstructorHasNoNormalInitializationExit() = assertSource("throw_only", true)
    @Test fun psiThrowPathsDoNotPolluteInitializedNormalExits() = assertSource("throw_or_initialize", false)
    @Test fun lightTreeThrowPathsDoNotPolluteInitializedNormalExits() = assertSource("throw_or_initialize", true)
    @Test fun psiFieldInitializersAndConstructorAssignmentsCompose() = assertSource("initializer_and_constructor", false)
    @Test fun lightTreeFieldInitializersAndConstructorAssignmentsCompose() = assertSource("initializer_and_constructor", true)
    @Test fun psiGeneratedParentConstructorKeepsFieldInitializerFacts() = assertSource("generated_parent", false)
    @Test fun lightTreeGeneratedParentConstructorKeepsFieldInitializerFacts() = assertSource("generated_parent", true)
    @Test fun psiThisDelegationCompletesTheActualParentConstructor() = assertSource("multiple_all_initialized", false)
    @Test fun lightTreeThisDelegationCompletesTheActualParentConstructor() = assertSource("multiple_all_initialized", true)
    @Test fun psiPrivateParentFieldDoesNotLeakIntoChildInitialization() = assertSource("private_uninitialized", false)
    @Test fun lightTreePrivateParentFieldDoesNotLeakIntoChildInitialization() = assertSource("private_uninitialized", true)
    @Test fun psiPrivateParentErrorRetainsItsActualSourceFile() = assertSource("private_cross_file", false)
    @Test fun lightTreePrivateParentErrorRetainsItsActualSourceFile() = assertSource("private_cross_file", true)
    @Test fun psiPrivateParentAndChildFieldsKeepSeparateIdentities() = assertSource("private_same_name", false)
    @Test fun lightTreePrivateParentAndChildFieldsKeepSeparateIdentities() = assertSource("private_same_name", true)
    @Test fun psiInitializedParentDoesNotInitializeTheChildOwnField() = assertSource("child_uninitialized", false)
    @Test fun lightTreeInitializedParentDoesNotInitializeTheChildOwnField() = assertSource("child_uninitialized", true)

    private fun assertSource(id: String, lightTree: Boolean) {
        val fixture = FIXTURES.single { it.id == id }
        val directory = Files.createDirectories(tempDir.resolve(id + if (lightTree) "-light-tree" else "-psi"))
        val paths = fixture.inputs.associate { input ->
            assertEquals(input.nativeSha256, sha256(input.text), "源码必须保持 cjc 核证过的原 body")
            input.name to directory.resolve(input.name).also { Files.writeString(it, input.text, StandardCharsets.UTF_8) }
        }
        val disposable = Disposer.newDisposable("parent-initialization-source-lifecycle")
        try {
            val messages = RecordingMessageCollector()
            val diagnostics = DiagnosticsCollectorImpl()
            val configuration = CompilerConfiguration().apply {
                messageCollector = messages
                diagnosticsCollector = diagnostics
                moduleName = "parent_initialization_source"
                useLightTree = lightTree
                assertTrue(configureLanguageVersionSettings("1.1.3"))
                fixture.inputs.forEach { addCangJieSourceRoot(paths.getValue(it.name).toString()) }
            }
            val artifact = CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable))
            val records = diagnostics.diagnosticsByFilePath.flatMap { (file, values) ->
                values.map { DiagnosticRecord(file, it) }
            }
            val description = records.joinToString("\n") { it.diagnostic.factoryName + ": " + it.diagnostic.renderMessage() } +
                "\nmessages=" + messages.messages
            assertNotNull(artifact, "正常 SOURCE checker 链必须产生 frontend 产品：$description")
            assertFalse(messages.hasErrors(), description)
            // executePhase 不执行 postActions，使用与 two-phase helper 相同的正式错误判据。
            val completed = artifact != null && !CheckCompilationErrors.CheckDiagnosticCollector.checkHasErrors(configuration)
            assertEquals(fixture.expected.isEmpty(), completed, description)
            val output = checkNotNull(artifact).frontendOutput.outputs.single()
            assertEquals(CfirSession.Kind.Source, output.session.kind)
            val files = output.fir.associateBy { normalize(Path.of(checkNotNull(checkNotNull(it.sourceFile).path))) }
            assertEquals(paths.values.map(::normalize).toSet(), files.keys, "完整 SOURCE 文件集合不能缩成当前子文件")

            for (input in fixture.inputs) {
                assertSourceProduct(input, files.getValue(normalize(paths.getValue(input.name))), output.session)
            }
            val owners = output.fir.flatMap { it.declarations.filterIsInstance<CfirClass>() }.associateBy { it.name.asString() }
            val parent = owners.getValue("Parent")
            if (fixture.id == "throw_only") {
                val field = parent.declarations.filterIsInstance<CfirFieldVariable>().single()
                assertFalse(field.exportFacts.isInitialized, "无正常构造出口不能以空集合给字段授 initialized=true")
            }
            val child = owners["Child"]
            if (child != null) {
                val parentTypes = child.superTypeRefs.mapNotNull { (it as? CfirResolvedTypeRef)?.coneType as? ConeClassLikeType }
                    .filter { it.lookupTag.classId.shortClassName.asString() == "Parent" }
                assertEquals(1, parentTypes.size)
                assertSame(parent.symbol, parentTypes.single().lookupTag.toSymbol(output.session), "父类型必须绑定实际已登记的 Source owner")
            }
            for (name in fixture.generatedOwners) {
                val constructor = owners.getValue(name).declarations.filterIsInstance<CfirConstructor>().single()
                assertTrue(constructor.valueParameters.isEmpty(), "实际 generated default 必须是无参 ctor")
            }
            assertDiagnostics(fixture, paths, records, description)
        } finally {
            Disposer.dispose(disposable)
        }
    }

    /** 只读取正式 parser/body/provider 产品，不通过 fixture 重建 flow、成员匹配或初始化归并。 */
    private fun assertSourceProduct(input: SourceInput, file: CfirFile, session: CfirSession) {
        assertTrue(file.origin.fromSource)
        assertSame(session, file.moduleData.session)
        assertNotNull(file.sourceFileLinesMapping)
        val parser = checkNotNull(file.parsingSourceContext)
        assertEquals(input.text, parser.snapshot.text)
        parser.requireInput(input.text)
        assertTrue(checkNotNull(file.parsingDiagnosticPublication).diagnostics.none { it.isError })
        val owners = file.declarations.filterIsInstance<CfirClass>()
        assertEquals(input.classNames, owners.map { it.name.asString() }, "保留原声明顺序，不能通过排序父类回避依赖")
        var explicitConstructors = 0
        for (owner in owners) {
            assertTrue(owner.resolvePhase >= CfirResolvePhase.BODY_RESOLVE)
            assertSame(file, session.cfirProvider.getCfirClassifierContainerFileIfAny(owner.symbol))
            val fields = owner.declarations.filterIsInstance<CfirFieldVariable>()
            val expectedFields = input.fields.getValue(owner.name.asString())
            assertEquals(expectedFields.map { it.name }, fields.map { it.name.asString() }, "只保留该实际 owner 自己声明的字段")
            for ((field, expected) in fields.zip(expectedFields)) {
                assertSame(file.moduleData, field.moduleData)
                assertTrue(field.returnTypeRef is CfirResolvedTypeRef)
                val initializer = field.initializer
                if (expected.initializer == null) {
                    assertNull(initializer)
                } else {
                    assertNotNull(initializer)
                    assertEquals(expected.initializer, sourceText(input.text, checkNotNull(checkNotNull(initializer).source)))
                }
            }
            for (constructor in owner.declarations.filterIsInstance<CfirConstructor>()) {
                val source = checkNotNull(constructor.source)
                if (source.kind !== CjRealSourceElementKind) continue
                val declaration = sourceText(input.text, source)
                if (!declaration.startsWith("public init(")) continue
                explicitConstructors++
                val body = checkNotNull(constructor.body) { "真实 init body 不能被 header/空 body 替换" }
                val actualBody = sourceText(input.text, checkNotNull(body.source))
                val expectedBody = declaration.substring(declaration.indexOf('{')).trim()
                assertEquals(expectedBody, actualBody, "ctor body 的 Source 范围必须保留原全文")
            }
        }
        assertEquals(input.explicitConstructors, explicitConstructors, "全部 actual ctor 必须保留")

        val observed = mutableListOf<BodyStatement>()
        file.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this)
            }
            private fun record(kind: String, source: CjSourceElement?) {
                if (source != null && source.kind === CjRealSourceElementKind) {
                    observed += BodyStatement(kind, sourceText(input.text, source))
                }
            }
            override fun visitAssignment(assignment: CfirAssignment) {
                record("assignment", assignment.source)
                super.visitAssignment(assignment)
            }
            override fun visitReturnExpression(returnExpression: CfirReturnExpression) {
                record("return", returnExpression.source)
                super.visitReturnExpression(returnExpression)
            }
            override fun visitThrowExpression(throwExpression: CfirThrowExpression) {
                record("throw", throwExpression.source)
                super.visitThrowExpression(throwExpression)
            }
        })
        assertEquals(input.statements.sortedBy(BodyStatement::sortKey), observed.sortedBy(BodyStatement::sortKey),
            "真实 assignment/return/throw IR 必须来自核证源码；不能只保留 parser 文本")
    }

    /** 诊断名称、字段参数、文件身份、名称 token 范围及无 note 期待均以完整多重集核验。 */
    private fun assertDiagnostics(fixture: Fixture, paths: Map<String, Path>, records: List<DiagnosticRecord>, description: String) {
        val expected = fixture.expected.map { expectation ->
            val input = fixture.inputs.single { it.name == expectation.file }
            val anchorStart = input.text.indexOf(expectation.anchor)
            require(anchorStart >= 0 && anchorStart == input.text.lastIndexOf(expectation.anchor))
            val tokenStart = anchorStart + expectation.anchor.indexOf(expectation.token)
            Key(expectation.factory, normalize(paths.getValue(expectation.file)), TextRange(tokenStart, tokenStart + expectation.token.length))
        }
        val actual = records.map { record ->
            val diagnostic = record.diagnostic
            assertTrue(diagnostic.severity.isError, description)
            assertTrue(diagnostic.isValid, description)
            assertInstanceOf(CjDiagnosticWithSource::class.java, diagnostic)
            Key(diagnostic.factoryName, normalize(Path.of(checkNotNull(record.filePath))), diagnostic.firstRange)
        }
        assertEquals(expected.sortedBy(Key::sortKey), actual.sortedBy(Key::sortKey), description)
        for ((index, expectation) in fixture.expected.withIndex()) {
            val diagnostic = records[actual.indexOf(expected[index])].diagnostic
            expectation.field?.let { name ->
                assertEquals(Name.identifier(name), assertInstanceOf(CjDiagnosticWithParameters1::class.java, diagnostic).a)
            }
            assertEquals(emptyList<CjDiagnosticRelatedInformation>(), diagnostic.relatedInformation, "固定 N 与原 cjc 都不附带 note")
        }
    }

    private fun sourceText(text: String, source: CjSourceElement): String {
        require(source.startOffset in 0..text.length && source.endOffset in source.startOffset..text.length)
        return text.substring(source.startOffset, source.endOffset).trim()
    }

    private fun normalize(path: Path): Path = path.toAbsolutePath().normalize()

    private data class SourceInput(
        val name: String,
        val text: String,
        val nativeSha256: String,
        val classNames: List<String>,
        val fields: Map<String, List<FieldInput>>,
        val explicitConstructors: Int,
        val statements: List<BodyStatement>,
    )
    private data class FieldInput(val name: String, val initializer: String? = null)
    private data class BodyStatement(val kind: String, val text: String) {
        fun sortKey(): String = kind + ":" + text
    }
    private data class ExpectedDiagnostic(val factory: String, val file: String, val anchor: String, val token: String, val field: String? = null)
    private data class Fixture(val id: String, val inputs: List<SourceInput>, val expected: List<ExpectedDiagnostic>, val generatedOwners: List<String> = emptyList())
    private data class DiagnosticRecord(val filePath: String?, val diagnostic: CjDiagnostic)
    private data class Key(val factory: String, val file: Path, val range: TextRange) {
        fun sortKey(): String = factory + ":" + file + ":" + range
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
                id = "declared_after",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package parent_initialization_source
                            
                            public class Child <: Parent {
                                public let copied: Int64
                            
                                public init() {
                                    super(1)
                                    this.copied = super.value
                                }
                            }
                            
                            public open class Parent {
                                public var value: Int64
                            
                                public init(n: Int64) {
                                    this.value = n
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "45513b683f9d30b18675f6c7155dbcdeea97105db0052ad8cd25d732a6f328dc",
                        classNames = listOf("Child", "Parent"),
                        fields = mapOf(
                            "Child" to listOf(FieldInput("copied")),
                            "Parent" to listOf(FieldInput("value")),
                        ),
                        explicitConstructors = 2,
                        statements = listOf(BodyStatement("assignment", "this.copied = super.value"), BodyStatement("assignment", "this.value = n")),
                    ),
                ),
                expected = listOf(),
            ),
            Fixture(
                id = "cross_file",
                inputs = listOf(
                    SourceInput(
                        name = "child.cj",
                        text = """
                            package parent_initialization_source
                            
                            public class Child <: Parent {
                                public let copied: Int64
                            
                                public init() {
                                    super(1)
                                    this.copied = super.value
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "0aedeb1c314ed608edfba3b76efe13464ad6a9f80db7e1bf3ac51bfc07f682a9",
                        classNames = listOf("Child"),
                        fields = mapOf(
                            "Child" to listOf(FieldInput("copied")),
                        ),
                        explicitConstructors = 1,
                        statements = listOf(BodyStatement("assignment", "this.copied = super.value")),
                    ),
                    SourceInput(
                        name = "parent.cj",
                        text = """
                            package parent_initialization_source
                            
                            public open class Parent {
                                public var value: Int64
                            
                                public init(n: Int64) {
                                    this.value = n
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "e4b8a38b7ffd7a8a533804a76598fe82a47fec6ffbfeb94a5d225b2a32210bb3",
                        classNames = listOf("Parent"),
                        fields = mapOf(
                            "Parent" to listOf(FieldInput("value")),
                        ),
                        explicitConstructors = 1,
                        statements = listOf(BodyStatement("assignment", "this.value = n")),
                    ),
                ),
                expected = listOf(),
            ),
            Fixture(
                id = "cross_file_reverse",
                inputs = listOf(
                    SourceInput(
                        name = "parent.cj",
                        text = """
                            package parent_initialization_source
                            
                            public open class Parent {
                                public var value: Int64
                            
                                public init(n: Int64) {
                                    this.value = n
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "e4b8a38b7ffd7a8a533804a76598fe82a47fec6ffbfeb94a5d225b2a32210bb3",
                        classNames = listOf("Parent"),
                        fields = mapOf(
                            "Parent" to listOf(FieldInput("value")),
                        ),
                        explicitConstructors = 1,
                        statements = listOf(BodyStatement("assignment", "this.value = n")),
                    ),
                    SourceInput(
                        name = "child.cj",
                        text = """
                            package parent_initialization_source
                            
                            public class Child <: Parent {
                                public let copied: Int64
                            
                                public init() {
                                    super(1)
                                    this.copied = super.value
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "0aedeb1c314ed608edfba3b76efe13464ad6a9f80db7e1bf3ac51bfc07f682a9",
                        classNames = listOf("Child"),
                        fields = mapOf(
                            "Child" to listOf(FieldInput("copied")),
                        ),
                        explicitConstructors = 1,
                        statements = listOf(BodyStatement("assignment", "this.copied = super.value")),
                    ),
                ),
                expected = listOf(),
            ),
            Fixture(
                id = "implicit_default",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package parent_initialization_source
                            
                            public open class Parent {
                                public var value: Int64
                            
                                public init(n: Int64) {
                                    this.value = n
                                }
                            }
                            
                            public class Child <: Parent {}
                        """.trimIndent() + "\n",
                        nativeSha256 = "a0a27a96ebf919c485462efdc3bfbc907cd3ef4fa33938f0e3e4f65d6ca81adc",
                        classNames = listOf("Parent", "Child"),
                        fields = mapOf(
                            "Parent" to listOf(FieldInput("value")),
                            "Child" to listOf(),
                        ),
                        explicitConstructors = 1,
                        statements = listOf(BodyStatement("assignment", "this.value = n")),
                    ),
                ),
                expected = listOf(ExpectedDiagnostic("CFIR_NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS", "source.cj", "public class Child <: Parent {}", "Child")),
                generatedOwners = listOf("Child"),
            ),
            Fixture(
                id = "multiple_isolated_bad_first",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package parent_initialization_source
                            
                            public open class Parent {
                                public var value: Int64
                            
                                public init() {
                                }
                            
                                public init(n: Int64) {
                                    this.value = n
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "0474422a8c8a286a6d893afbcac23e9b747ff7d3a8d59b637297020292a78dc6",
                        classNames = listOf("Parent"),
                        fields = mapOf(
                            "Parent" to listOf(FieldInput("value")),
                        ),
                        explicitConstructors = 2,
                        statements = listOf(BodyStatement("assignment", "this.value = n")),
                    ),
                ),
                expected = listOf(ExpectedDiagnostic("CFIR_CLASS_UNINITIALIZED_FIELD", "source.cj", "public init() {", "init", "value")),
            ),
            Fixture(
                id = "multiple_isolated_bad_last",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package parent_initialization_source
                            
                            public open class Parent {
                                public var value: Int64
                            
                                public init(n: Int64) {
                                    this.value = n
                                }
                            
                                public init() {
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "d9e21136c051ccc00efdb6dc1cefd2e34d72338ee98a926712a8dc9ea17cb59d",
                        classNames = listOf("Parent"),
                        fields = mapOf(
                            "Parent" to listOf(FieldInput("value")),
                        ),
                        explicitConstructors = 2,
                        statements = listOf(BodyStatement("assignment", "this.value = n")),
                    ),
                ),
                expected = listOf(ExpectedDiagnostic("CFIR_CLASS_UNINITIALIZED_FIELD", "source.cj", "public init() {", "init", "value")),
            ),
            Fixture(
                id = "child_writes_parent_field",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package parent_initialization_source
                            
                            public class Child <: Parent {
                                public let copied: Int64
                            
                                public init() {
                                    super(1)
                                    super.value = 2
                                    this.copied = super.value
                                }
                            }
                            
                            public open class Parent {
                                public var value: Int64
                            
                                public init(n: Int64) {
                                    this.value = n
                                }
                            
                                public init() {
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "b5c09e8024360460e53bfcd57b0030c05934980e7a0ce2926a2f9e3c4ec20786",
                        classNames = listOf("Child", "Parent"),
                        fields = mapOf(
                            "Child" to listOf(FieldInput("copied")),
                            "Parent" to listOf(FieldInput("value")),
                        ),
                        explicitConstructors = 3,
                        statements = listOf(BodyStatement("assignment", "super.value = 2"), BodyStatement("assignment", "this.copied = super.value"), BodyStatement("assignment", "this.value = n")),
                    ),
                ),
                expected = listOf(ExpectedDiagnostic("CFIR_CLASS_UNINITIALIZED_FIELD", "source.cj", "public init() {\n    }", "init", "value")),
            ),
            Fixture(
                id = "early_return",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package parent_initialization_source
                            
                            public open class Parent {
                                public var value: Int64
                            
                                public init(stop: Bool) {
                                    if (stop) {
                                        return
                                    }
                                    this.value = 1
                                }
                            }
                            
                            public class Child <: Parent {
                                public let copied: Int64
                            
                                public init() {
                                    super(false)
                                    this.copied = super.value
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "92f6830969c4bec528f915fee4a88517adb912886548cd75136d167c4c318cc5",
                        classNames = listOf("Parent", "Child"),
                        fields = mapOf(
                            "Parent" to listOf(FieldInput("value")),
                            "Child" to listOf(FieldInput("copied")),
                        ),
                        explicitConstructors = 2,
                        statements = listOf(BodyStatement("return", "return"), BodyStatement("assignment", "this.value = 1"), BodyStatement("assignment", "this.copied = super.value")),
                    ),
                ),
                expected = listOf(ExpectedDiagnostic("CFIR_CLASS_UNINITIALIZED_FIELD", "source.cj", "public init(stop: Bool) {", "init", "value")),
            ),
            Fixture(
                id = "early_return_initialized",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package parent_initialization_source
                            
                            public open class Parent {
                                public var value: Int64
                            
                                public init(stop: Bool) {
                                    this.value = 1
                                    if (stop) {
                                        return
                                    }
                                    this.value = 2
                                }
                            }
                            
                            public class Child <: Parent {
                                public let copied: Int64
                            
                                public init() {
                                    super(false)
                                    this.copied = super.value
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "26b90ae7994297c652f27d94818d5010403af96a01df7343dc1b76e2c6337097",
                        classNames = listOf("Parent", "Child"),
                        fields = mapOf(
                            "Parent" to listOf(FieldInput("value")),
                            "Child" to listOf(FieldInput("copied")),
                        ),
                        explicitConstructors = 2,
                        statements = listOf(BodyStatement("assignment", "this.value = 1"), BodyStatement("return", "return"), BodyStatement("assignment", "this.value = 2"), BodyStatement("assignment", "this.copied = super.value")),
                    ),
                ),
                expected = listOf(),
            ),
            Fixture(
                id = "throw_only",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package parent_initialization_source
                            
                            public open class Parent {
                                public var value: Int64
                            
                                public init(error: Exception) {
                                    throw error
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "70a98640166b317d538bc201df436028dc983e6ffb94261055573252f9e15cda",
                        classNames = listOf("Parent"),
                        fields = mapOf(
                            "Parent" to listOf(FieldInput("value")),
                        ),
                        explicitConstructors = 1,
                        statements = listOf(BodyStatement("throw", "throw error")),
                    ),
                ),
                expected = listOf(),
            ),
            Fixture(
                id = "throw_or_initialize",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package parent_initialization_source
                            
                            public open class Parent {
                                public var value: Int64
                            
                                public init(n: Int64, error: Exception) {
                                    if (n < 0) {
                                        throw error
                                    }
                                    this.value = n
                                }
                            }
                            
                            public class Child <: Parent {
                                public let copied: Int64
                            
                                public init(error: Exception) {
                                    super(1, error)
                                    this.copied = super.value
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "661c8bbbbb9a2933b404a688ea1f06153c4a49ccbe46a75fc398aacc4beef6a2",
                        classNames = listOf("Parent", "Child"),
                        fields = mapOf(
                            "Parent" to listOf(FieldInput("value")),
                            "Child" to listOf(FieldInput("copied")),
                        ),
                        explicitConstructors = 2,
                        statements = listOf(BodyStatement("throw", "throw error"), BodyStatement("assignment", "this.value = n"), BodyStatement("assignment", "this.copied = super.value")),
                    ),
                ),
                expected = listOf(),
            ),
            Fixture(
                id = "initializer_and_constructor",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package parent_initialization_source
                            
                            public open class Parent {
                                public var seed: Int64 = 2
                                public var value: Int64
                            
                                public init(n: Int64) {
                                    this.value = this.seed + n
                                }
                            }
                            
                            public class Child <: Parent {
                                public let ready: Int64 = 3
                                public let copied: Int64
                            
                                public init() {
                                    super(1)
                                    this.copied = super.value + this.ready
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "831b125ebab164017f9ebe448b8b34479a644895ee0de27e07c30b4e1c22902f",
                        classNames = listOf("Parent", "Child"),
                        fields = mapOf(
                            "Parent" to listOf(FieldInput("seed", "2"), FieldInput("value")),
                            "Child" to listOf(FieldInput("ready", "3"), FieldInput("copied")),
                        ),
                        explicitConstructors = 2,
                        statements = listOf(BodyStatement("assignment", "this.value = this.seed + n"), BodyStatement("assignment", "this.copied = super.value + this.ready")),
                    ),
                ),
                expected = listOf(),
            ),
            Fixture(
                id = "generated_parent",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package parent_initialization_source
                            
                            public open class Parent {
                                public var value: Int64 = 7
                            }
                            
                            public class Child <: Parent {
                                public let copied: Int64
                            
                                public init() {
                                    super()
                                    this.copied = super.value
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "b970205c4aae404078236b4682b40293809211effff342e70e96fda6a0e686ad",
                        classNames = listOf("Parent", "Child"),
                        fields = mapOf(
                            "Parent" to listOf(FieldInput("value", "7")),
                            "Child" to listOf(FieldInput("copied")),
                        ),
                        explicitConstructors = 1,
                        statements = listOf(BodyStatement("assignment", "this.copied = super.value")),
                    ),
                ),
                expected = listOf(),
                generatedOwners = listOf("Parent"),
            ),
            Fixture(
                id = "multiple_all_initialized",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package parent_initialization_source
                            
                            public open class Parent {
                                public var value: Int64
                            
                                public init() {
                                    this(1)
                                }
                            
                                public init(n: Int64) {
                                    this.value = n
                                }
                            }
                            
                            public class Child <: Parent {
                                public let copied: Int64
                            
                                public init() {
                                    super()
                                    this.copied = super.value
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "6edc1e579af42eefb204c3e58b18a4e4ef849fdb4f3f1a3fc7003da2c5740690",
                        classNames = listOf("Parent", "Child"),
                        fields = mapOf(
                            "Parent" to listOf(FieldInput("value")),
                            "Child" to listOf(FieldInput("copied")),
                        ),
                        explicitConstructors = 3,
                        statements = listOf(BodyStatement("assignment", "this.value = n"), BodyStatement("assignment", "this.copied = super.value")),
                    ),
                ),
                expected = listOf(),
            ),
            Fixture(
                id = "private_uninitialized",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package parent_initialization_source
                            
                            public open class Parent {
                                private var hidden: Int64
                            
                                public init() {
                                }
                            
                                public func read(): Int64 {
                                    return this.hidden
                                }
                            }
                            
                            public class Child <: Parent {
                                public let hidden: Int64 = 2
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "6c6a6a2b2e33d33e3a25cc2563208bab9d8ccaf1465ef0d258cb06fbb06f93c2",
                        classNames = listOf("Parent", "Child"),
                        fields = mapOf(
                            "Parent" to listOf(FieldInput("hidden")),
                            "Child" to listOf(FieldInput("hidden", "2")),
                        ),
                        explicitConstructors = 1,
                        statements = listOf(BodyStatement("return", "return this.hidden")),
                    ),
                ),
                expected = listOf(ExpectedDiagnostic("CFIR_CLASS_UNINITIALIZED_FIELD", "source.cj", "public init() {", "init", "hidden")),
                generatedOwners = listOf("Child"),
            ),
            Fixture(
                id = "private_cross_file",
                inputs = listOf(
                    SourceInput(
                        name = "child.cj",
                        text = """
                            package parent_initialization_source
                            
                            public class Child <: Parent {
                                public let hidden: Int64 = 2
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "3c09bc8231f8b3d794cef372a7fca9288eb94d2143bb786769045df682a0172b",
                        classNames = listOf("Child"),
                        fields = mapOf(
                            "Child" to listOf(FieldInput("hidden", "2")),
                        ),
                        explicitConstructors = 0,
                        statements = listOf(),
                    ),
                    SourceInput(
                        name = "parent.cj",
                        text = """
                            package parent_initialization_source
                            
                            public open class Parent {
                                private var hidden: Int64
                            
                                public init() {
                                }
                            
                                public func read(): Int64 {
                                    return this.hidden
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "aedc407ffb88bf9befeb76002bf093662955071fc2e4e2ce88cefe9b82cdcd52",
                        classNames = listOf("Parent"),
                        fields = mapOf(
                            "Parent" to listOf(FieldInput("hidden")),
                        ),
                        explicitConstructors = 1,
                        statements = listOf(BodyStatement("return", "return this.hidden")),
                    ),
                ),
                expected = listOf(ExpectedDiagnostic("CFIR_CLASS_UNINITIALIZED_FIELD", "parent.cj", "public init() {", "init", "hidden")),
                generatedOwners = listOf("Child"),
            ),
            Fixture(
                id = "private_same_name",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package parent_initialization_source
                            
                            public open class Parent {
                                private var hidden: Int64
                            
                                public init(n: Int64) {
                                    this.hidden = n
                                }
                            
                                public func read(): Int64 {
                                    return this.hidden
                                }
                            }
                            
                            public class Child <: Parent {
                                public let hidden: Int64
                            
                                public init() {
                                    super(1)
                                    this.hidden = 2
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "01c0209045cf7dd7dca9ea47068f1be13833ae2e16614d997ebf4495222229db",
                        classNames = listOf("Parent", "Child"),
                        fields = mapOf(
                            "Parent" to listOf(FieldInput("hidden")),
                            "Child" to listOf(FieldInput("hidden")),
                        ),
                        explicitConstructors = 2,
                        statements = listOf(BodyStatement("assignment", "this.hidden = n"), BodyStatement("return", "return this.hidden"), BodyStatement("assignment", "this.hidden = 2")),
                    ),
                ),
                expected = listOf(),
            ),
            Fixture(
                id = "child_uninitialized",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package parent_initialization_source
                            
                            public open class Parent {
                                public var value: Int64
                            
                                public init(n: Int64) {
                                    this.value = n
                                }
                            }
                            
                            public class Child <: Parent {
                                public let copied: Int64
                            
                                public init() {
                                    super(1)
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "2cd3385e057cb6d86115aae77701638ae63b0af5e91d77264a7d443ba89b2c14",
                        classNames = listOf("Parent", "Child"),
                        fields = mapOf(
                            "Parent" to listOf(FieldInput("value")),
                            "Child" to listOf(FieldInput("copied")),
                        ),
                        explicitConstructors = 2,
                        statements = listOf(BodyStatement("assignment", "this.value = n")),
                    ),
                ),
                expected = listOf(ExpectedDiagnostic("CFIR_CLASS_UNINITIALIZED_FIELD", "source.cj", "public init() {", "init", "copied")),
            ),
        )
    }
}
