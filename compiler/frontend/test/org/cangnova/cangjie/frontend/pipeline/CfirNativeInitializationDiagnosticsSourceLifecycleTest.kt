package org.cangnova.cangjie.frontend.pipeline

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassInitializationInfo
import org.cangnova.cangjie.cfir.declarations.CfirClassInitializationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirConstructorInitializationAvailability
import org.cangnova.cangjie.cfir.declarations.CfirFieldInitializationState
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNativeInitializationDiagnosticAvailability
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.classInitializationInfo
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
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
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
 * R20 的实际 SOURCE 产品回归：构造出口、native 静态末边界和字段义务必须同时保持各自含义。
 *
 * 固定 K=3c7a5041f238102285764d370665f56f587f9dc4 的完整 class/NormalPath 消费框架，
 * 与固定 N=14276740ffec1262b81b6b5236b1d840af270b37 的父先检查、same-owner 读取资格、
 * return 义务和继承残余域分别核证，详见 r20-early-return-parent-obligation-cascade-authority.md。
 * 八个实际 cjc 编译的原文、SHA 和完整 stdout/stderr 保存在
 * build/cjmp-invisible-peer-20261125/r20-native-diagnostic-source-writer-cjc.json。
 *
 * 测试只调用普通 frontend 入口，再读取 classInitializationInfo(session).nativeStaticDiagnostics。
 * 不构造或发布 Info、flow、graph、字段状态或诊断，不调用 private checker，不依赖旧测试的 fixture。
 * CLASS_UNINITIALIZED_FIELD 定位 init，USED_BEFORE_INITIALIZATION 定位 own；native 旧 point
 * 范围另存原 JSON。完整多重集保留父错误、自有缺字段和同 owner UBI，不过滤额外错误。
 */
@OptIn(CompilerConfiguration.Internals::class, CfirImplementationDetail::class)
class CfirNativeInitializationDiagnosticsSourceLifecycleTest {
    @TempDir
    lateinit var tempDir: Path

    @Test fun psiFalseSuperArgumentKeepsCanonicalAndStaticProductsSeparate() = assertSource("direct_super_false", false)
    @Test fun lightTreeFalseSuperArgumentKeepsCanonicalAndStaticProductsSeparate() = assertSource("direct_super_false", true)
    @Test fun psiTrueSuperArgumentDoesNotSpecializeParentObligations() = assertSource("direct_super_true", false)
    @Test fun lightTreeTrueSuperArgumentDoesNotSpecializeParentObligations() = assertSource("direct_super_true", true)
    @Test fun psiParentFailureDoesNotSuppressChildOwnObligation() = assertSource("child_own_uninitialized", false)
    @Test fun lightTreeParentFailureDoesNotSuppressChildOwnObligation() = assertSource("child_own_uninitialized", true)
    @Test fun psiOwnReadStillReportsUbiWhileCrossOwnerSuperReadDoesNot() = assertSource("own_read_before_write", false)
    @Test fun lightTreeOwnReadStillReportsUbiWhileCrossOwnerSuperReadDoesNot() = assertSource("own_read_before_write", true)
    @Test fun psiInitializedOwnReadDoesNotAcquireUbi() = assertSource("own_read_after_write", false)
    @Test fun lightTreeInitializedOwnReadDoesNotAcquireUbi() = assertSource("own_read_after_write", true)
    @Test fun psiInitializedEarlyReturnHasNoFailedReturnObligation() = assertSource("parent_initialized_before_return", false)
    @Test fun lightTreeInitializedEarlyReturnHasNoFailedReturnObligation() = assertSource("parent_initialized_before_return", true)
    @Test fun psiChildFirstSourceRootsPreserveActualParentAndOwnObligation() = assertSource("cross_file_child_first", false)
    @Test fun lightTreeChildFirstSourceRootsPreserveActualParentAndOwnObligation() = assertSource("cross_file_child_first", true)
    @Test fun psiParentFirstSourceRootsPreserveActualParentAndOwnObligation() = assertSource("cross_file_parent_first", false)
    @Test fun lightTreeParentFirstSourceRootsPreserveActualParentAndOwnObligation() = assertSource("cross_file_parent_first", true)

    private fun assertSource(id: String, lightTree: Boolean) {
        val fixture = FIXTURES.single { it.id == id }
        val directory = Files.createDirectories(tempDir.resolve(id + if (lightTree) "-light-tree" else "-psi"))
        val paths = fixture.inputs.associate { input ->
            assertEquals(input.nativeSha256, sha256(input.text), "SOURCE 必须保持实际 cjc 核证原文")
            input.name to directory.resolve(input.name).also { Files.writeString(it, input.text, StandardCharsets.UTF_8) }
        }
        val disposable = Disposer.newDisposable("native-initialization-diagnostics-source")
        try {
            val messages = RecordingMessageCollector()
            val diagnostics = DiagnosticsCollectorImpl()
            val configuration = CompilerConfiguration().apply {
                messageCollector = messages
                diagnosticsCollector = diagnostics
                moduleName = "initialization_boundary"
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
            assertNotNull(artifact, description)
            assertFalse(messages.hasErrors(), description)
            val output = checkNotNull(artifact).frontendOutput.outputs.single()
            val session = output.session
            assertEquals(CfirSession.Kind.Source, session.kind)
            val files = output.fir.associateBy { normalize(Path.of(checkNotNull(checkNotNull(it.sourceFile).path))) }
            assertEquals(paths.values.map(::normalize).toSet(), files.keys, "不得遗漏另一实际 SOURCE 文件")

            for (input in fixture.inputs) {
                assertSourceFile(input, files.getValue(normalize(paths.getValue(input.name))), session)
            }
            val owners = output.fir.flatMap { it.declarations.filterIsInstance<CfirClass>() }
            assertEquals(setOf("Parent", "Child"), owners.map { it.name.asString() }.toSet())
            assertEquals(2, owners.size)
            val parent = owners.single { it.name.asString() == "Parent" }
            val child = owners.single { it.name.asString() == "Child" }
            val parentTypes = child.superTypeRefs.mapNotNull { (it as? CfirResolvedTypeRef)?.coneType as? ConeClassLikeType }
                .filter { it.lookupTag.classId.shortClassName.asString() == "Parent" }
            assertEquals(1, parentTypes.size)
            assertSame(parent.symbol, parentTypes.single().lookupTag.toSymbol(session), "依赖边必须绑定真实父 owner")

            val parentInfo = requireSourceInfo(parent, session, description)
            val childInfo = requireSourceInfo(child, session, description)
            assertSame(parentInfo, childInfo.parents.single { it.owner === parent }, "父产品必须是同一会话已发布实例")
            assertParentProducts(fixture, parent, parentInfo)
            assertChildProducts(fixture, parent, child, parentInfo, childInfo)
            assertResolvedReads(fixture, parent, child)
            assertDiagnostics(fixture, paths, records, description)
            val completed = !CheckCompilationErrors.CheckDiagnosticCollector.checkHasErrors(configuration)
            assertEquals(fixture.expected.isEmpty(), completed, description)

            // 全部消费结束后再读取，防止 native 诊断用途反写 canonical 或令产品退役。
            assertSame(parentInfo, parent.classInitializationInfo(session))
            assertSame(childInfo, child.classInitializationInfo(session))
            val value = parent.declarations.filterIsInstance<CfirFieldVariable>().single()
            assertState(parentInfo.fieldState(value.symbol), fixture.parentInitializedBeforeReturn, true)
            assertEquals(fixture.parentInitializedBeforeReturn, value.exportFacts.isInitialized)
            assertTrue(parentInfo.isCurrent())
            assertTrue(childInfo.isCurrent())
        } finally {
            Disposer.dispose(disposable)
        }
    }

    /** 只读实际 parser、provider、body 与 IR；输入顺序不能被父类排序替换。 */
    private fun assertSourceFile(input: SourceInput, file: CfirFile, session: CfirSession) {
        assertTrue(file.origin.fromSource)
        assertSame(session, file.moduleData.session)
        assertNotNull(file.sourceFileLinesMapping)
        val parser = checkNotNull(file.parsingSourceContext)
        assertEquals(input.text, parser.snapshot.text)
        parser.requireInput(input.text)
        assertTrue(checkNotNull(file.parsingDiagnosticPublication).diagnostics.none { it.isError })
        val owners = file.declarations.filterIsInstance<CfirClass>()
        assertEquals(input.classNames, owners.map { it.name.asString() })
        for (owner in owners) {
            assertTrue(owner.resolvePhase >= CfirResolvePhase.BODY_RESOLVE)
            assertSame(file, session.cfirProvider.getCfirClassifierContainerFileIfAny(owner.symbol))
            val fields = owner.declarations.filterIsInstance<CfirFieldVariable>()
            assertEquals(input.fields.getValue(owner.name.asString()), fields.map { it.name.asString() })
            for (field in fields) {
                assertSame(file.moduleData, field.moduleData)
                assertTrue(field.isVar)
                assertNull(field.initializer, "核证输入没有字段 initializer")
                assertTrue(field.returnTypeRef is CfirResolvedTypeRef)
            }
            val constructor = owner.declarations.filterIsInstance<CfirConstructor>().single()
            assertTrue(constructor.origin.fromSource)
            assertFalse(constructor.isPrimary)
            assertFalse(constructor.exportFacts.isCompilerAdded)
            assertFalse(constructor.exportFacts.isImplicitlyAdded)
            assertSame(CjRealSourceElementKind, checkNotNull(constructor.source).kind)
            assertEquals(if (owner.name.asString() == "Parent") listOf("stop") else emptyList<String>(),
                constructor.valueParameters.map { it.name.asString() })
            val declarationText = sourceText(input.text, checkNotNull(constructor.source))
            assertTrue(declarationText.startsWith("public init("))
            val body = checkNotNull(constructor.body)
            assertEquals(declarationText.substring(declarationText.indexOf('{')).trim(),
                sourceText(input.text, checkNotNull(body.source)), "原 ctor body 必须完整保留")
        }
        val observed = mutableListOf<String>()
        file.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this)
            }
            override fun visitAssignment(assignment: CfirAssignment) {
                observed += sourceText(input.text, checkNotNull(assignment.source))
                super.visitAssignment(assignment)
            }
            override fun visitReturnExpression(returnExpression: CfirReturnExpression) {
                if (returnExpression.source?.kind === CjRealSourceElementKind) {
                    observed += sourceText(input.text, checkNotNull(returnExpression.source))
                }
                super.visitReturnExpression(returnExpression)
            }
        })
        assertEquals(input.statements.sorted(), observed.sorted(), "assignment/return 必须是实际 IR")
    }

    private fun requireSourceInfo(owner: CfirClass, session: CfirSession, description: String): CfirClassInitializationInfo {
        val info = checkNotNull(owner.classInitializationInfo(session)) { "正式 owner 产品尚未发布：$description" }
        assertSame(owner, info.owner)
        assertSame(session, info.useSiteSession)
        assertEquals(CfirClassInitializationOrigin.SOURCE, info.origin)
        assertTrue(info.isCurrent())
        assertFalse(info.hasUnavailableConstructors)
        assertTrue(info.hasNormalConstructionExit)
        val constructor = owner.declarations.filterIsInstance<CfirConstructor>().single()
        assertEquals(setOf(constructor.symbol), info.constructors.keys)
        val canonical = info.constructors.getValue(constructor.symbol)
        assertSame(constructor, canonical.constructor)
        assertSame(owner.symbol, canonical.receiverOwner)
        assertEquals(CfirConstructorInitializationAvailability.COMPLETE, canonical.availability)
        assertTrue(canonical.hasNormalExit)
        assertTrue(canonical.flow.delegationReturnsNormally, "缺字段诊断不能被当作构造不返回")
        assertTrue(canonical.flow.initializesOwner)
        assertTrue(canonical.flow.initializerFields.isEmpty())
        assertTrue(canonical.flow.parameterFields.isEmpty())

        val native = info.nativeStaticDiagnostics
        assertEquals(CfirNativeInitializationDiagnosticAvailability.COMPLETE, native.availability)
        assertEquals(setOf(constructor.symbol), native.constructors.keys)
        val nativeConstructor = native.constructors.getValue(constructor.symbol)
        assertSame(constructor, nativeConstructor.constructor)
        assertEquals(0, nativeConstructor.ordinal)
        assertEquals(CfirConstructorInitializationAvailability.COMPLETE, nativeConstructor.availability)
        assertTrue(nativeConstructor.flow.delegationReturnsNormally)
        assertTrue(nativeConstructor.flow.initializesOwner)
        assertTrue(nativeConstructor.flow.initializerFields.isEmpty())
        assertTrue(nativeConstructor.flow.parameterFields.isEmpty())
        for (field in owner.declarations.filterIsInstance<CfirFieldVariable>()) {
            assertState(info.initialFields.getValue(field.symbol), false, false)
            assertState(canonical.flow.beforeDelegation.getValue(field.symbol), false, false)
            assertState(canonical.flow.afterDelegation.getValue(field.symbol), false, false)
            assertState(nativeConstructor.flow.beforeDelegation.getValue(field.symbol), false, false)
            assertState(nativeConstructor.flow.afterDelegation.getValue(field.symbol), false, false)
        }
        return info
    }

    /** 父正常出口的交集 false/并集 true 与 native 末 true 必须同时存在。 */
    private fun assertParentProducts(fixture: Fixture, parent: CfirClass, info: CfirClassInitializationInfo) {
        val value = parent.declarations.filterIsInstance<CfirFieldVariable>().single()
        val constructor = parent.declarations.filterIsInstance<CfirConstructor>().single()
        val canonical = info.constructors.getValue(constructor.symbol)
        val native = info.nativeStaticDiagnostics
        val nativeConstructor = native.constructors.getValue(constructor.symbol)
        assertEquals(2, canonical.normalExitFields.size, "条件 return 与 body 末顺序各有实际正常出口")
        val exitStates = canonical.normalExitFields.map { exit ->
            val state = exit.getValue(value.symbol)
            assertEquals(state.definitelyInitialized, state.possiblyInitialized, "单条出口不是构造交集")
            state.definitelyInitialized
        }
        assertEquals(if (fixture.parentInitializedBeforeReturn) listOf(true, true) else listOf(false, true), exitStates.sorted())
        assertState(info.fields.getValue(value.symbol), fixture.parentInitializedBeforeReturn, true)
        assertState(canonical.terminalFields.getValue(value.symbol), true, true)
        assertState(native.fields.getValue(value.symbol), true, true)
        assertState(nativeConstructor.endFields.getValue(value.symbol), true, true)
        assertEquals(fixture.parentInitializedBeforeReturn, value.exportFacts.isInitialized,
            "native 末位没有 canonical INITIALIZED 发布权限")

        assertEquals(setOf(value.symbol), native.obligations.map { it.field.symbol }.toSet())
        assertEquals(1, native.obligations.size)
        val obligation = native.obligations.single()
        assertSame(value, obligation.field)
        assertSame(parent, obligation.checkingOwner)
        assertNull(obligation.parentBoundary, "父自己的字段义务没有转交父边界")
        val returns = realReturns(constructor)
        assertEquals(1, returns.size)
        if (fixture.parentInitializedBeforeReturn) {
            assertTrue(nativeConstructor.firstReturnUninitialized.isEmpty())
        } else {
            assertEquals(setOf(value.symbol), nativeConstructor.firstReturnUninitialized.keys)
            assertSame(returns.single(), nativeConstructor.firstReturnUninitialized.getValue(value.symbol),
                "失败义务必须保留实际 early return IR 身份")
            assertEquals("return", sourceText(fixture.inputFor("Parent").text, checkNotNull(returns.single().source)))
        }
    }

    /** tracked 继承 storage 和 completeness 义务分别核对，绝不能因父有错跳过 child。 */
    private fun assertChildProducts(
        fixture: Fixture,
        parent: CfirClass,
        child: CfirClass,
        parentInfo: CfirClassInitializationInfo,
        info: CfirClassInitializationInfo,
    ) {
        val value = parent.declarations.filterIsInstance<CfirFieldVariable>().single()
        val fields = child.declarations.filterIsInstance<CfirFieldVariable>()
        val constructor = child.declarations.filterIsInstance<CfirConstructor>().single()
        val canonical = info.constructors.getValue(constructor.symbol)
        val native = info.nativeStaticDiagnostics
        val nativeConstructor = native.constructors.getValue(constructor.symbol)
        assertSame(parentInfo, info.parents.single { it.owner === parent })
        assertState(info.fieldState(value.symbol), fixture.parentInitializedBeforeReturn, true)
        assertState(canonical.normalExitFields.single().getValue(value.symbol), fixture.parentInitializedBeforeReturn, true)
        assertState(canonical.flow.afterDelegation.getValue(value.symbol), fixture.parentInitializedBeforeReturn, true)
        assertState(native.fields.getValue(value.symbol), true, true)
        assertState(nativeConstructor.flow.afterDelegation.getValue(value.symbol), true, true)
        assertState(nativeConstructor.endFields.getValue(value.symbol), true, true)
        assertTrue(nativeConstructor.firstReturnUninitialized.isEmpty())
        assertTrue(realReturns(constructor).isEmpty())

        assertEquals(fields.map { it.symbol }.toSet(), native.obligations.map { it.field.symbol }.toSet())
        assertEquals(fields.size, native.obligations.size, "父静态末 true 不转交 value；own 候选不能全部取消")
        for (obligation in native.obligations) {
            assertSame(fields.single { it.symbol === obligation.field.symbol }, obligation.field)
            assertSame(child, obligation.checkingOwner)
            assertNull(obligation.parentBoundary, "这些都是真实 child own 义务")
            assertNotSame(value, obligation.field)
        }
        for (field in fields) {
            val initialized = field.name.asString() != "missing"
            assertState(info.fieldState(field.symbol), initialized, initialized)
            assertState(canonical.normalExitFields.single().getValue(field.symbol), initialized, initialized)
            assertState(native.fields.getValue(field.symbol), initialized, initialized)
            assertState(nativeConstructor.endFields.getValue(field.symbol), initialized, initialized)
            assertEquals(initialized, field.exportFacts.isInitialized)
        }

        val body = checkNotNull(constructor.body)
        val superCall = checkNotNull(canonical.flow.delegationCall)
        assertSame(superCall, nativeConstructor.flow.delegationCall, "两个 purpose 必须使用同一个真实 delegated call")
        assertEquals("super(" + fixture.superArgument + ")", sourceText(fixture.inputFor("Child").text, checkNotNull(superCall.source)))
        val calls = mutableListOf<CfirFunctionCall>()
        body.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this)
            }
            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                calls += functionCall
                super.visitFunctionCall(functionCall)
            }
        })
        assertTrue(calls.any { it === superCall }, "flow 不能引用 fixture 构造或复制的 call")
        assertSame(parent.declarations.filterIsInstance<CfirConstructor>().single().symbol, canonical.delegationTarget)
    }

    /** 读取资格的负/正控制都必须绑定实际 storage，不能仅按 super/own 文本跳过诊断。 */
    private fun assertResolvedReads(fixture: Fixture, parent: CfirClass, child: CfirClass) {
        val value = parent.declarations.filterIsInstance<CfirFieldVariable>().single()
        val input = fixture.inputFor("Child")
        val assignments = mutableListOf<CfirAssignment>()
        checkNotNull(child.declarations.filterIsInstance<CfirConstructor>().single().body).accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this)
            }
            override fun visitAssignment(assignment: CfirAssignment) {
                assignments += assignment
                super.visitAssignment(assignment)
            }
        })
        val copied = assignments.single { sourceText(input.text, checkNotNull(it.source)) == "this.copied = super.value" }
        val superRead = assertInstanceOf(CfirQualifiedAccessExpression::class.java, copied.rValue)
        assertEquals("super.value", sourceText(input.text, checkNotNull(superRead.source)))
        val superTarget = assertInstanceOf(CfirResolvedNamedReference::class.java, superRead.calleeReference).resolvedSymbol
        assertSame(value.symbol, assertInstanceOf(CfirVariableSymbol::class.java, superTarget).unwrapSubstitutionOverrides())
        val own = child.declarations.filterIsInstance<CfirFieldVariable>().singleOrNull { it.name.asString() == "own" }
        if (own != null) {
            val observed = assignments.single { sourceText(input.text, checkNotNull(it.source)) == "this.observed = this.own" }
            val ownRead = assertInstanceOf(CfirQualifiedAccessExpression::class.java, observed.rValue)
            assertEquals("this.own", sourceText(input.text, checkNotNull(ownRead.source)))
            val ownTarget = assertInstanceOf(CfirResolvedNamedReference::class.java, ownRead.calleeReference).resolvedSymbol
            assertSame(own.symbol, assertInstanceOf(CfirVariableSymbol::class.java, ownTarget).unwrapSubstitutionOverrides())
        }
    }

    private fun realReturns(constructor: CfirConstructor): List<CfirReturnExpression> {
        val returns = mutableListOf<CfirReturnExpression>()
        checkNotNull(constructor.body).accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this)
            }
            override fun visitReturnExpression(returnExpression: CfirReturnExpression) {
                if (returnExpression.source?.kind === CjRealSourceElementKind) returns += returnExpression
                super.visitReturnExpression(returnExpression)
            }
        })
        return returns
    }

    /** 完整 factory/file/range/field 多重集，不删除 parent 错误或掩盖 child 的附加错误。 */
    private fun assertDiagnostics(fixture: Fixture, paths: Map<String, Path>, records: List<DiagnosticRecord>, description: String) {
        val expected = fixture.expected.map { expectation ->
            val text = fixture.inputs.single { it.name == expectation.file }.text
            val lines = text.split('\n')
            val start = lines.take(expectation.line - 1).sumOf { it.length + 1 } + expectation.column - 1
            assertEquals(expectation.token, text.substring(start, start + expectation.token.length))
            Key(expectation.factory, normalize(paths.getValue(expectation.file)), TextRange(start, start + expectation.token.length),
                Name.identifier(expectation.field))
        }
        val actual = records.map { record ->
            val diagnostic = record.diagnostic
            assertTrue(diagnostic.severity.isError, description)
            assertTrue(diagnostic.isValid, description)
            assertInstanceOf(CjDiagnosticWithSource::class.java, diagnostic)
            val parameter = assertInstanceOf(CjDiagnosticWithParameters1::class.java, diagnostic).a
            assertInstanceOf(Name::class.java, parameter)
            assertEquals(emptyList<CjDiagnosticRelatedInformation>(), diagnostic.relatedInformation)
            Key(diagnostic.factoryName, normalize(Path.of(checkNotNull(record.filePath))), diagnostic.firstRange, parameter as Name)
        }
        assertEquals(expected.sortedBy(Key::sortKey), actual.sortedBy(Key::sortKey), description)
    }

    private fun assertState(state: CfirFieldInitializationState, definite: Boolean, possible: Boolean) {
        assertEquals(definite, state.definitelyInitialized)
        assertEquals(possible, state.possiblyInitialized)
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
        val fields: Map<String, List<String>>,
        val statements: List<String>,
    )
    private data class ExpectedDiagnostic(
        val factory: String,
        val field: String,
        val file: String,
        val line: Int,
        val column: Int,
        val token: String,
    )
    private data class Fixture(
        val id: String,
        val inputs: List<SourceInput>,
        val expected: List<ExpectedDiagnostic>,
        val superArgument: String,
        val parentInitializedBeforeReturn: Boolean = false,
    ) {
        fun inputFor(owner: String): SourceInput = inputs.single { owner in it.classNames }
    }
    private data class DiagnosticRecord(val filePath: String?, val diagnostic: CjDiagnostic)
    private data class Key(val factory: String, val file: Path, val range: TextRange, val field: Name) {
        fun sortKey(): String = factory + ":" + file + ":" + range + ":" + field
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
                id = "direct_super_false",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package initialization_boundary
                            
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
                                public var copied: Int64
                            
                                public init() {
                                    super(false)
                                    this.copied = super.value
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "58fc2bcf4b2eef3746500cb75623ee02304655d52e95c3f2efe900eaa8993d32",
                        classNames = listOf("Parent", "Child"),
                        fields = mapOf("Parent" to listOf("value"), "Child" to listOf("copied")),
                        statements = listOf("return", "this.value = 1", "this.copied = super.value"),
                    ),
                ),
                expected = listOf(
                    ExpectedDiagnostic("CFIR_CLASS_UNINITIALIZED_FIELD", "value", "source.cj", 6, 12, "init"),
                ),
                superArgument = "false",
                parentInitializedBeforeReturn = false,
            ),
            Fixture(
                id = "direct_super_true",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package initialization_boundary
                            
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
                                public var copied: Int64
                            
                                public init() {
                                    super(true)
                                    this.copied = super.value
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "a2c1a25a26711bef1ca6082e70bb8c89b2c6a1d391ad4a86fdabafc357d5909a",
                        classNames = listOf("Parent", "Child"),
                        fields = mapOf("Parent" to listOf("value"), "Child" to listOf("copied")),
                        statements = listOf("return", "this.value = 1", "this.copied = super.value"),
                    ),
                ),
                expected = listOf(
                    ExpectedDiagnostic("CFIR_CLASS_UNINITIALIZED_FIELD", "value", "source.cj", 6, 12, "init"),
                ),
                superArgument = "true",
                parentInitializedBeforeReturn = false,
            ),
            Fixture(
                id = "child_own_uninitialized",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package initialization_boundary
                            
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
                                public var copied: Int64
                                public var missing: Int64
                            
                                public init() {
                                    super(false)
                                    this.copied = super.value
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "60d9795974aa0bd10ddcb91cd563da7b2aa27e6fde6572efff66bc7ad7bf2e58",
                        classNames = listOf("Parent", "Child"),
                        fields = mapOf("Parent" to listOf("value"), "Child" to listOf("copied", "missing")),
                        statements = listOf("return", "this.value = 1", "this.copied = super.value"),
                    ),
                ),
                expected = listOf(
                    ExpectedDiagnostic("CFIR_CLASS_UNINITIALIZED_FIELD", "value", "source.cj", 6, 12, "init"),
                    ExpectedDiagnostic("CFIR_CLASS_UNINITIALIZED_FIELD", "missing", "source.cj", 18, 12, "init"),
                ),
                superArgument = "false",
                parentInitializedBeforeReturn = false,
            ),
            Fixture(
                id = "own_read_before_write",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package initialization_boundary
                            
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
                                public var copied: Int64
                                public var own: Int64
                                public var observed: Int64
                            
                                public init() {
                                    super(false)
                                    this.copied = super.value
                                    this.observed = this.own
                                    this.own = 2
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "ad9169dbed1326bb1fb372726ee4db4b52efd5fc3c66285599c152fa67b68bff",
                        classNames = listOf("Parent", "Child"),
                        fields = mapOf("Parent" to listOf("value"), "Child" to listOf("copied", "own", "observed")),
                        statements = listOf("return", "this.value = 1", "this.copied = super.value", "this.observed = this.own", "this.own = 2"),
                    ),
                ),
                expected = listOf(
                    ExpectedDiagnostic("CFIR_CLASS_UNINITIALIZED_FIELD", "value", "source.cj", 6, 12, "init"),
                    ExpectedDiagnostic("CFIR_USED_BEFORE_INITIALIZATION", "own", "source.cj", 22, 30, "own"),
                ),
                superArgument = "false",
                parentInitializedBeforeReturn = false,
            ),
            Fixture(
                id = "own_read_after_write",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package initialization_boundary
                            
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
                                public var copied: Int64
                                public var own: Int64
                                public var observed: Int64
                            
                                public init() {
                                    super(false)
                                    this.copied = super.value
                                    this.own = 2
                                    this.observed = this.own
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "7cc5de063aebfda29729ce6f9fc5a501432e5f58531b33def7a2485c439a02b8",
                        classNames = listOf("Parent", "Child"),
                        fields = mapOf("Parent" to listOf("value"), "Child" to listOf("copied", "own", "observed")),
                        statements = listOf("return", "this.value = 1", "this.copied = super.value", "this.own = 2", "this.observed = this.own"),
                    ),
                ),
                expected = listOf(
                    ExpectedDiagnostic("CFIR_CLASS_UNINITIALIZED_FIELD", "value", "source.cj", 6, 12, "init"),
                ),
                superArgument = "false",
                parentInitializedBeforeReturn = false,
            ),
            Fixture(
                id = "parent_initialized_before_return",
                inputs = listOf(
                    SourceInput(
                        name = "source.cj",
                        text = """
                            package initialization_boundary
                            
                            public open class Parent {
                                public var value: Int64
                            
                                public init(stop: Bool) {
                                    this.value = 1
                                    if (stop) {
                                        return
                                    }
                                }
                            }
                            
                            public class Child <: Parent {
                                public var copied: Int64
                            
                                public init() {
                                    super(true)
                                    this.copied = super.value
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "6ff3a0a613abb14320224f417b5a7b09562f8c4b6c38a81911024c43710bb334",
                        classNames = listOf("Parent", "Child"),
                        fields = mapOf("Parent" to listOf("value"), "Child" to listOf("copied")),
                        statements = listOf("this.value = 1", "return", "this.copied = super.value"),
                    ),
                ),
                expected = emptyList(),
                superArgument = "true",
                parentInitializedBeforeReturn = true,
            ),
            Fixture(
                id = "cross_file_child_first",
                inputs = listOf(
                    SourceInput(
                        name = "child.cj",
                        text = """
                            package initialization_boundary
                            
                            public class Child <: Parent {
                                public var copied: Int64
                                public var missing: Int64
                            
                                public init() {
                                    super(false)
                                    this.copied = super.value
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "7ccc3686fa52ae33b45fa63f3a806534076a46c78c7fb8a898ce4533570f99b5",
                        classNames = listOf("Child"),
                        fields = mapOf("Child" to listOf("copied", "missing")),
                        statements = listOf("this.copied = super.value"),
                    ),
                    SourceInput(
                        name = "parent.cj",
                        text = """
                            package initialization_boundary
                            
                            public open class Parent {
                                public var value: Int64
                            
                                public init(stop: Bool) {
                                    if (stop) {
                                        return
                                    }
                                    this.value = 1
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "63504c43ea51038bd839e8c48990bf8f65fe65a62f5ca241e5177c0e9ae1ce6e",
                        classNames = listOf("Parent"),
                        fields = mapOf("Parent" to listOf("value")),
                        statements = listOf("return", "this.value = 1"),
                    ),
                ),
                expected = listOf(
                    ExpectedDiagnostic("CFIR_CLASS_UNINITIALIZED_FIELD", "value", "parent.cj", 6, 12, "init"),
                    ExpectedDiagnostic("CFIR_CLASS_UNINITIALIZED_FIELD", "missing", "child.cj", 7, 12, "init"),
                ),
                superArgument = "false",
                parentInitializedBeforeReturn = false,
            ),
            Fixture(
                id = "cross_file_parent_first",
                inputs = listOf(
                    SourceInput(
                        name = "parent.cj",
                        text = """
                            package initialization_boundary
                            
                            public open class Parent {
                                public var value: Int64
                            
                                public init(stop: Bool) {
                                    if (stop) {
                                        return
                                    }
                                    this.value = 1
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "63504c43ea51038bd839e8c48990bf8f65fe65a62f5ca241e5177c0e9ae1ce6e",
                        classNames = listOf("Parent"),
                        fields = mapOf("Parent" to listOf("value")),
                        statements = listOf("return", "this.value = 1"),
                    ),
                    SourceInput(
                        name = "child.cj",
                        text = """
                            package initialization_boundary
                            
                            public class Child <: Parent {
                                public var copied: Int64
                                public var missing: Int64
                            
                                public init() {
                                    super(false)
                                    this.copied = super.value
                                }
                            }
                        """.trimIndent() + "\n",
                        nativeSha256 = "7ccc3686fa52ae33b45fa63f3a806534076a46c78c7fb8a898ce4533570f99b5",
                        classNames = listOf("Child"),
                        fields = mapOf("Child" to listOf("copied", "missing")),
                        statements = listOf("this.copied = super.value"),
                    ),
                ),
                expected = listOf(
                    ExpectedDiagnostic("CFIR_CLASS_UNINITIALIZED_FIELD", "value", "parent.cj", 6, 12, "init"),
                    ExpectedDiagnostic("CFIR_CLASS_UNINITIALIZED_FIELD", "missing", "child.cj", 7, 12, "init"),
                ),
                superArgument = "false",
                parentInitializedBeforeReturn = false,
            ),
        )
    }
}
